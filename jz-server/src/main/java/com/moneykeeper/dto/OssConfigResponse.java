package com.moneykeeper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OssConfigResponse {
    private Long id;
    private String provider;
    private String name;
    private String endpoint;
    private String region;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private Boolean pathStyleAccess;
    private String publicDomain;
    private String prefix;
    private Boolean isActive;
}
