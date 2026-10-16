package com.moneykeeper.service;

import com.moneykeeper.dto.AppFileUploadResponse;
import com.moneykeeper.dto.AppReleaseRequest;
import com.moneykeeper.dto.AppReleaseResponse;
import com.moneykeeper.dto.AppUpdateResponse;
import com.moneykeeper.entity.AppRelease;
import com.moneykeeper.repository.AppReleaseRepository;
import com.moneykeeper.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppReleaseService {

    private final AppReleaseRepository appReleaseRepository;
    private final S3StorageService s3StorageService;

    public List<AppReleaseResponse> listReleases(String platform) {
        List<AppRelease> releases;
        if (platform != null && !platform.isBlank()) {
            releases = appReleaseRepository.findByPlatformOrderByVersionCodeDesc(platform);
        } else {
            releases = appReleaseRepository.findAll();
        }
        return releases.stream().map(this::toResponse).toList();
    }

    public AppReleaseResponse getRelease(Long id) {
        AppRelease release = appReleaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("版本记录不存在"));
        return toResponse(release);
    }

    /**
     * 第一步：上传APP文件到OSS，返回文件信息
     */
    public AppFileUploadResponse uploadFile(MultipartFile file, String platform) {
        S3StorageService.UploadResult uploadResult = s3StorageService.upload(file, "app/" + platform);
        log.info("APP文件上传成功: {}, size={}", uploadResult.url(), uploadResult.fileSize());
        return new AppFileUploadResponse(
                uploadResult.url(),
                uploadResult.fullKey(),
                uploadResult.fileSize(),
                file.getOriginalFilename()
        );
    }

    /**
     * 第二步：创建版本发布记录（文件已上传，只保存元数据）
     */
    @Transactional
    public AppReleaseResponse createRelease(AppReleaseRequest request) {
        if (appReleaseRepository.existsByVersionNameAndPlatform(request.getVersionName(), request.getPlatform())) {
            throw new IllegalArgumentException("该平台已存在相同版本号 " + request.getVersionName());
        }

        AppRelease release = new AppRelease();
        release.setVersionName(request.getVersionName());
        release.setVersionCode(request.getVersionCode());
        release.setPlatform(request.getPlatform());
        release.setReleaseNotes(request.getReleaseNotes());
        release.setDownloadUrl(request.getDownloadUrl());
        release.setFileSize(request.getFileSize());
        release.setFileKey(request.getFileKey());
        release.setIsForceUpdate(request.getIsForceUpdate());
        release.setIsPublished(false);

        AppRelease saved = appReleaseRepository.save(release);
        log.info("创建APP发布记录: version={}, platform={}", saved.getVersionName(), saved.getPlatform());
        return toResponse(saved);
    }

    @Transactional
    public AppReleaseResponse publishRelease(Long id) {
        AppRelease release = appReleaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("版本记录不存在"));

        release.setIsPublished(true);
        release.setPublishedAt(LocalDateTime.now());
        AppRelease saved = appReleaseRepository.save(release);
        log.info("发布APP版本: version={}", saved.getVersionName());
        return toResponse(saved);
    }

    @Transactional
    public AppReleaseResponse unpublishRelease(Long id) {
        AppRelease release = appReleaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("版本记录不存在"));

        release.setIsPublished(false);
        release.setPublishedAt(null);
        AppRelease saved = appReleaseRepository.save(release);
        return toResponse(saved);
    }

    @Transactional
    public void deleteRelease(Long id) {
        AppRelease release = appReleaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("版本记录不存在"));

        // 删除OSS上的文件
        if (release.getFileKey() != null) {
            try {
                s3StorageService.delete(release.getFileKey());
            } catch (Exception e) {
                log.warn("删除OSS文件失败: {}", release.getFileKey(), e);
            }
        }

        appReleaseRepository.delete(release);
        log.info("删除APP发布记录: version={}", release.getVersionName());
    }

    /**
     * 检查APP更新（供uni-app调用）
     * 
     * 对比逻辑（优先语义版本号，versionCode 作为辅助）：
     *   1. 先比较 versionName 语义版本（如 "1.0.3" > "1.0.2"）→ 有更新
     *   2. 若 versionName 相同，再比较 versionCode 数值 → versionCode 更高则有更新（热修复场景）
     *   3. 否则无更新
     * 
     * 优先 versionName 的原因：Admin 创建发布记录时可能填写的 versionCode 与 APP manifest.json 不一致，
     * 而 versionName（如 "1.0.2" → "1.0.3"）才是真正的版本标识。
     */
    public AppUpdateResponse checkUpdate(String platform, Integer currentVersionCode, String currentVersionName) {
        List<AppRelease> published = appReleaseRepository
                .findByPlatformAndIsPublishedTrueOrderByVersionCodeDesc(platform != null ? platform : "android");
        log.info("升级检查: platform={}, 手机 versionName={}, versionCode={}, 已发布数量={}",
                platform, currentVersionName, currentVersionCode, published.size());
        if (published.isEmpty()) {
            log.warn("升级检查: 该平台({})无已发布版本记录，返回无更新", platform);
            return new AppUpdateResponse(false, null, null, null, null, false);
        }

        // 取 versionCode 最高的已发布版本作为"最新"
        AppRelease latest = published.get(0);
        log.info("升级检查: 服务端最新版本 versionName={}, versionCode={}", latest.getVersionName(), latest.getVersionCode());

        boolean hasUpdate = false;

        // 主判断：versionName 语义版本比较（优先）
        if (currentVersionName != null && latest.getVersionName() != null) {
            int serverSemVer = semVerToInt(latest.getVersionName());
            int phoneSemVer = semVerToInt(currentVersionName);
            if (serverSemVer > phoneSemVer) {
                hasUpdate = true;
                log.info("升级检查: 语义版本号发现更新: server={} (semver={}) > phone={} (semver={})",
                        latest.getVersionName(), serverSemVer, currentVersionName, phoneSemVer);
            }
        }

        // 辅助判断：versionName 相同时，versionCode 更高也算有更新（热修复场景）
        if (!hasUpdate && latest.getVersionCode() > currentVersionCode) {
            hasUpdate = true;
            log.info("升级检查: versionName相同但versionCode更高: server_vc={} > phone_vc={}",
                    latest.getVersionCode(), currentVersionCode);
        }

        if (!hasUpdate) {
            log.info("升级检查: 无更新 - 服务端 versionName={}, versionCode={} vs 手机 versionName={}, versionCode={}",
                    latest.getVersionName(), latest.getVersionCode(), currentVersionName, currentVersionCode);
        }

        return new AppUpdateResponse(
                hasUpdate,
                latest.getVersionName(),
                latest.getVersionCode(),
                hasUpdate ? latest.getDownloadUrl() : null,
                hasUpdate ? latest.getReleaseNotes() : null,
                hasUpdate && Boolean.TRUE.equals(latest.getIsForceUpdate())
        );
    }

    /**
     * 将 "x.y.z" 格式版本号转为可比较整数（major*10000 + minor*100 + patch）
     * 支持跨版本：1.0.1→10001, 2.0.0→20000, 1.10.5→11005
     */
    private int semVerToInt(String version) {
        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return major * 10000 + minor * 100 + patch;
        } catch (Exception e) {
            return 0;
        }
    }

    private AppReleaseResponse toResponse(AppRelease release) {
        return new AppReleaseResponse(
                release.getId(),
                release.getVersionName(),
                release.getVersionCode(),
                release.getPlatform(),
                release.getReleaseNotes(),
                release.getDownloadUrl(),
                release.getFileSize(),
                release.getFileKey(),
                release.getIsForceUpdate(),
                release.getIsPublished(),
                release.getPublishedAt(),
                release.getCreatedAt()
        );
    }
}
