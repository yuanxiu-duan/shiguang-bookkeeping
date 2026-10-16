package com.moneykeeper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppUpdateResponse {
    private Boolean hasUpdate;
    private String versionName;
    private Integer versionCode;
    private String downloadUrl;
    private String releaseNotes;
    private Boolean isForceUpdate;
}
