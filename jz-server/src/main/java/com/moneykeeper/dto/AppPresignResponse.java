package com.moneykeeper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 直传（预签名）响应：uploadUrl 是一次性 PUT 地址，浏览器拿到后直接把文件传进对象存储，
 * 数据不经过本服务器，能绕开服务器出网带宽瓶颈。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppPresignResponse {
    /** 一次性预签名 PUT 地址（含签名，短时有效） */
    private String uploadUrl;
    /** 对象存储里的完整 key */
    private String fileKey;
    /** 上传完成后的公开访问地址 */
    private String downloadUrl;
    private String fileName;
    private Long fileSize;
    /** 上传时必须原样携带的 Content-Type（已签名，不一致会被拒绝） */
    private String contentType;
    /** 预签名有效期（秒） */
    private long expiresIn;
}
