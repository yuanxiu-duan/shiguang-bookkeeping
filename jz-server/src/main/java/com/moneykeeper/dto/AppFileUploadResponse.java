package com.moneykeeper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppFileUploadResponse {
    private String downloadUrl;
    private String fileKey;
    private Long fileSize;
    private String fileName;
}
