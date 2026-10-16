package com.moneykeeper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppReleaseResponse {
    private Long id;
    private String versionName;
    private Integer versionCode;
    private String platform;
    private String releaseNotes;
    private String downloadUrl;
    private Long fileSize;
    private String fileKey;
    private Boolean isForceUpdate;
    private Boolean isPublished;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
