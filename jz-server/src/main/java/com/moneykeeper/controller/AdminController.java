package com.moneykeeper.controller;

import com.moneykeeper.dto.*;
import com.moneykeeper.entity.User;
import com.moneykeeper.service.AppReleaseService;
import com.moneykeeper.service.OssConfigService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OssConfigService ossConfigService;
    private final AppReleaseService appReleaseService;

    public AdminController(OssConfigService ossConfigService, AppReleaseService appReleaseService) {
        this.ossConfigService = ossConfigService;
        this.appReleaseService = appReleaseService;
    }

    // ========== OSS配置管理 ==========

    @GetMapping("/oss-configs")
    public Result<List<OssConfigResponse>> listOssConfigs() {
        return Result.success(ossConfigService.listConfigs());
    }

    @GetMapping("/oss-configs/{id}")
    public Result<OssConfigResponse> getOssConfig(@PathVariable Long id) {
        return Result.success(ossConfigService.getConfig(id));
    }

    @PostMapping("/oss-configs")
    public Result<OssConfigResponse> createOssConfig(@Valid @RequestBody OssConfigRequest request) {
        return Result.success(ossConfigService.createConfig(request));
    }

    @PutMapping("/oss-configs/{id}")
    public Result<OssConfigResponse> updateOssConfig(@PathVariable Long id, @Valid @RequestBody OssConfigRequest request) {
        return Result.success(ossConfigService.updateConfig(id, request));
    }

    @DeleteMapping("/oss-configs/{id}")
    public Result<Void> deleteOssConfig(@PathVariable Long id) {
        ossConfigService.deleteConfig(id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/oss-configs/{id}/activate")
    public Result<OssConfigResponse> activateOssConfig(@PathVariable Long id) {
        return Result.success(ossConfigService.activateConfig(id));
    }

    @PostMapping("/oss-configs/{id}/test")
    public Result<String> testOssConnection(@PathVariable Long id) {
        Map<String, String> debugInfo = ossConfigService.testConnection(id);
        String error = debugInfo.get("error");
        if (error.isEmpty()) {
            return Result.success("连接成功", "ok");
        }
        String detail = String.format("连接失败 [provider=%s, endpoint=%s, region=%s, bucket=%s, pathStyle=%s]: %s",
                debugInfo.get("provider"), debugInfo.get("endpoint"),
                debugInfo.get("region"), debugInfo.get("bucket"),
                debugInfo.get("pathStyle"), error);
        return Result.error(detail);
    }

    // ========== APP发布管理 ==========

    @GetMapping("/app-releases")
    public Result<List<AppReleaseResponse>> listAppReleases(
            @RequestParam(required = false) String platform) {
        return Result.success(appReleaseService.listReleases(platform));
    }

    @GetMapping("/app-releases/{id}")
    public Result<AppReleaseResponse> getAppRelease(@PathVariable Long id) {
        return Result.success(appReleaseService.getRelease(id));
    }

    /**
     * 第一步：上传APP文件到OSS，返回文件信息
     */
    @PostMapping("/app-releases/upload-file")
    public Result<AppFileUploadResponse> uploadAppFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "android") String platform) {
        return Result.success(appReleaseService.uploadFile(file, platform));
    }

    /**
     * 第二步：提交版本发布信息（JSON body，包含文件上传返回的信息）
     */
    @PostMapping("/app-releases")
    public Result<AppReleaseResponse> createAppRelease(
            @Valid @RequestBody AppReleaseRequest request) {
        return Result.success(appReleaseService.createRelease(request));
    }

    @PutMapping("/app-releases/{id}/publish")
    public Result<AppReleaseResponse> publishAppRelease(@PathVariable Long id) {
        return Result.success(appReleaseService.publishRelease(id));
    }

    @PutMapping("/app-releases/{id}/unpublish")
    public Result<AppReleaseResponse> unpublishAppRelease(@PathVariable Long id) {
        return Result.success(appReleaseService.unpublishRelease(id));
    }

    @DeleteMapping("/app-releases/{id}")
    public Result<Void> deleteAppRelease(@PathVariable Long id) {
        appReleaseService.deleteRelease(id);
        return Result.success("删除成功", null);
    }

    // ========== APP更新检查（供uni-app调用，无需认证） ==========

    @GetMapping("/app-releases/check-update")
    public Result<AppUpdateResponse> checkUpdate(
            @RequestParam(defaultValue = "android") String platform,
            @RequestParam Integer versionCode,
            @RequestParam(required = false) String versionName) {
        return Result.success(appReleaseService.checkUpdate(platform, versionCode, versionName));
    }
}
