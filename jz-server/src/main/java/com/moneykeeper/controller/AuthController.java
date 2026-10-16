package com.moneykeeper.controller;

import com.moneykeeper.dto.*;
import com.moneykeeper.entity.User;
import com.moneykeeper.service.AuthService;
import com.moneykeeper.service.EmailService;
import com.moneykeeper.util.RsaUtil;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final RsaUtil rsaUtil;

    public AuthController(AuthService authService, EmailService emailService, RsaUtil rsaUtil) {
        this.authService = authService;
        this.emailService = emailService;
        this.rsaUtil = rsaUtil;
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        emailService.sendVerificationCode(request.getEmail(), request.getType());
        return Result.success("验证码已发送，请注意查收", null);
    }

    @GetMapping("/me")
    public Result<UserResponse> getCurrentUser(@AuthenticationPrincipal User user) {
        return Result.success(authService.getCurrentUser(user));
    }

    /**
     * 修改昵称
     */
    @PutMapping("/nickname")
    public Result<UserResponse> updateNickname(@AuthenticationPrincipal User user,
                                               @Valid @RequestBody UpdateNicknameRequest request) {
        return Result.success(authService.updateNickname(user, request.getNickname()));
    }

    /**
     * 修改头像（multipart 上传，已排除 CryptoFilter）
     */
    @PostMapping("/avatar")
    public Result<UserResponse> updateAvatar(@AuthenticationPrincipal User user,
                                             @RequestParam("file") MultipartFile file) {
        return Result.success(authService.updateAvatar(user, file));
    }

    /**
     * 修改邮箱（需先验证旧邮箱身份确认码，再验证新邮箱验证码）
     */
    @PutMapping("/email")
    public Result<UserResponse> updateEmail(@AuthenticationPrincipal User user,
                                            @Valid @RequestBody UpdateEmailRequest request) {
        return Result.success(authService.updateEmail(user, request.getOldEmailCode(), request.getNewEmail(), request.getNewEmailCode()));
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> updatePassword(@AuthenticationPrincipal User user,
                                       @Valid @RequestBody UpdatePasswordRequest request) {
        authService.updatePassword(user, request.getNewPassword(), request.getConfirmPassword());
        return Result.success("密码修改成功", null);
    }

    /**
     * 找回密码（无需登录）
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getCode(),
                request.getNewPassword(), request.getConfirmPassword());
        return Result.success("密码重置成功", null);
    }

    /**
     * 获取 RSA 公钥（前端用于加密 AES 密钥）
     * 此接口无需认证，且被 CryptoFilter 排除
     */
    @GetMapping("/public-key")
    public Result<String> getPublicKey() {
        return Result.success(rsaUtil.getPublicKeyPem());
    }
}