package com.moneykeeper.service;

import com.moneykeeper.dto.OssConfigRequest;
import com.moneykeeper.dto.OssConfigResponse;
import com.moneykeeper.entity.OssConfig;
import com.moneykeeper.repository.OssConfigRepository;
import com.moneykeeper.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssConfigService {

    private final OssConfigRepository ossConfigRepository;
    private final S3StorageService s3StorageService;

    public List<OssConfigResponse> listConfigs() {
        return ossConfigRepository.findAll().stream().map(this::toResponse).toList();
    }

    public OssConfigResponse getConfig(Long id) {
        OssConfig config = ossConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("OSS配置不存在"));
        return toResponse(config);
    }

    @Transactional
    public OssConfigResponse createConfig(OssConfigRequest request) {
        OssConfig config = new OssConfig();
        copyFromRequest(config, request);
        OssConfig saved = ossConfigRepository.save(config);
        log.info("创建OSS配置: provider={}, name={}", saved.getProvider(), saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public OssConfigResponse updateConfig(Long id, OssConfigRequest request) {
        OssConfig config = ossConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("OSS配置不存在"));
        copyFromRequest(config, request);
        OssConfig saved = ossConfigRepository.save(config);
        log.info("更新OSS配置: id={}", id);
        return toResponse(saved);
    }

    @Transactional
    public void deleteConfig(Long id) {
        OssConfig config = ossConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("OSS配置不存在"));
        if (Boolean.TRUE.equals(config.getIsActive())) {
            throw new IllegalArgumentException("不能删除当前激活的OSS配置");
        }
        ossConfigRepository.delete(config);
        log.info("删除OSS配置: id={}", id);
    }

    @Transactional
    public OssConfigResponse activateConfig(Long id) {
        // 先取消所有激活状态
        ossConfigRepository.findAll().forEach(c -> {
            if (Boolean.TRUE.equals(c.getIsActive())) {
                c.setIsActive(false);
                ossConfigRepository.save(c);
            }
        });

        OssConfig config = ossConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("OSS配置不存在"));
        config.setIsActive(true);
        OssConfig saved = ossConfigRepository.save(config);
        log.info("激活OSS配置: id={}", id);
        return toResponse(saved);
    }

    public Map<String, String> testConnection(Long id) {
        OssConfig config = ossConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("OSS配置不存在"));
        String error = s3StorageService.testConnection(config);
        Map<String, String> result = new java.util.LinkedHashMap<>();
        result.put("error", error != null ? error : "");
        result.put("provider", config.getProvider() != null ? config.getProvider() : "");
        result.put("endpoint", config.getEndpoint() != null ? config.getEndpoint() : "");
        result.put("region", config.getRegion() != null ? config.getRegion() : "");
        result.put("bucket", config.getBucketName() != null ? config.getBucketName() : "");
        result.put("pathStyle", String.valueOf(config.getPathStyleAccess()));
        return result;
    }

    private void copyFromRequest(OssConfig config, OssConfigRequest request) {
        config.setProvider(request.getProvider());
        config.setName(request.getName());
        config.setEndpoint(request.getEndpoint());
        config.setRegion(request.getRegion());
        config.setAccessKey(request.getAccessKey());
        config.setSecretKey(request.getSecretKey());
        config.setBucketName(request.getBucketName());
        config.setPathStyleAccess(request.getPathStyleAccess());
        config.setPublicDomain(request.getPublicDomain());
        config.setPrefix(request.getPrefix());
    }

    private OssConfigResponse toResponse(OssConfig config) {
        // 脱敏：secretKey只显示前后4位
        String maskedSecret = maskSecret(config.getSecretKey());
        return new OssConfigResponse(
                config.getId(),
                config.getProvider(),
                config.getName(),
                config.getEndpoint(),
                config.getRegion(),
                config.getAccessKey(),
                maskedSecret,
                config.getBucketName(),
                config.getPathStyleAccess(),
                config.getPublicDomain(),
                config.getPrefix(),
                config.getIsActive()
        );
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }
}
