package com.moneykeeper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppReleaseRequest {
    @NotBlank(message = "版本号不能为空")
    private String versionName;

    @NotNull(message = "版本编号不能为空")
    private Integer versionCode;

    private String platform = "android";

    private String releaseNotes;

    private Boolean isForceUpdate = false;

    // 文件上传后返回的信息
    @NotBlank(message = "下载地址不能为空")
    private String downloadUrl;

    @NotBlank(message = "文件Key不能为空")
    private String fileKey;

    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    private String fileName;
}
