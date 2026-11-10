package com.moneykeeper.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 申请直传（预签名）请求：浏览器告诉服务端要传什么文件，服务端返回一次性上传地址。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppPresignRequest {

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小不合法")
    private Long fileSize;
}
