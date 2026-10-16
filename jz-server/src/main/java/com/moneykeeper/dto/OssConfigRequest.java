package com.moneykeeper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OssConfigRequest {
    @NotBlank(message = "OSS提供商不能为空")
    private String provider; // ALIYUN, TENCENT, QINIU, MINIO, AWS

    @NotBlank(message = "配置名称不能为空")
    private String name;

    @NotBlank(message = "endpoint不能为空")
    private String endpoint;

    private String region = "us-east-1";

    @NotBlank(message = "accessKey不能为空")
    private String accessKey;

    @NotBlank(message = "secretKey不能为空")
    private String secretKey;

    @NotBlank(message = "bucketName不能为空")
    private String bucketName;

    private Boolean pathStyleAccess = true;

    private String publicDomain;

    private String prefix = "";
}
