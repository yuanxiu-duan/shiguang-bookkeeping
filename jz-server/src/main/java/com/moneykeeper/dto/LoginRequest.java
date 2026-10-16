package com.moneykeeper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    private String username;  // 用户名（与email二选一）

    private String password;  // 密码（与emailCode二选一）

    private String email;     // 邮箱（与username二选一）

    private String emailCode; // 邮箱验证码（与password二选一）

    @NotBlank(message = "登录类型不能为空")
    private String loginType; // PASSWORD: 账号/邮箱+密码登录, CODE: 邮箱+验证码登录
}