package com.moneykeeper.service;

import com.moneykeeper.dto.LoginRequest;
import com.moneykeeper.dto.LoginResponse;
import com.moneykeeper.dto.RegisterRequest;
import com.moneykeeper.dto.UserResponse;
import com.moneykeeper.entity.AccountBook;
import com.moneykeeper.entity.User;
import com.moneykeeper.entity.VerificationCode;
import com.moneykeeper.repository.UserRepository;
import com.moneykeeper.repository.VerificationCodeRepository;
import com.moneykeeper.storage.S3StorageService;
import com.moneykeeper.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AccountBookService accountBookService;
    private final CategoryService categoryService;
    private final VerificationCodeRepository verificationCodeRepository;
    private final S3StorageService s3StorageService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       AccountBookService accountBookService, CategoryService categoryService,
                       VerificationCodeRepository verificationCodeRepository,
                       S3StorageService s3StorageService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.accountBookService = accountBookService;
        this.categoryService = categoryService;
        this.verificationCodeRepository = verificationCodeRepository;
        this.s3StorageService = s3StorageService;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 如果填写了邮箱，需要验证邮箱验证码
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (request.getEmailCode() == null || request.getEmailCode().isBlank()) {
                throw new IllegalArgumentException("请填写邮箱验证码");
            }
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("该邮箱已被注册");
            }
            verifyCode(request.getEmail(), request.getEmailCode(), "REGISTER");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole("USER");

        User savedUser = userRepository.save(user);

        AccountBook defaultBook = accountBookService.createDefaultAccountBook(savedUser.getId());
        categoryService.initDefaultCategories(savedUser.getId(), defaultBook.getId());

        String token = jwtUtil.generateToken(savedUser.getUsername());

        return new LoginResponse(token, toUserResponse(savedUser));
    }

    public LoginResponse login(LoginRequest request) {
        if ("CODE".equals(request.getLoginType())) {
            // 邮箱+验证码登录
            return loginByEmailCode(request);
        } else {
            // 账号/邮箱+密码登录
            return loginByPassword(request);
        }
    }

    private LoginResponse loginByPassword(LoginRequest request) {
        User user = null;

        // 优先用用户名查找，其次用邮箱查找
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user = userRepository.findByUsername(request.getUsername())
                    .orElse(null);
        }

        if (user == null && request.getEmail() != null && !request.getEmail().isBlank()) {
            user = userRepository.findByEmail(request.getEmail())
                    .orElse(null);
        }

        if (user == null) {
            throw new IllegalArgumentException("用户名/邮箱或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名/邮箱或密码错误");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        return new LoginResponse(token, toUserResponse(user));
    }

    private LoginResponse loginByEmailCode(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("请输入邮箱");
        }
        if (request.getEmailCode() == null || request.getEmailCode().isBlank()) {
            throw new IllegalArgumentException("请输入验证码");
        }

        verifyCode(request.getEmail(), request.getEmailCode(), "LOGIN");

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("该邮箱未注册"));

        String token = jwtUtil.generateToken(user.getUsername());
        return new LoginResponse(token, toUserResponse(user));
    }

    /**
     * 校验验证码
     */
    private void verifyCode(String email, String code, String type) {
        VerificationCode record = verificationCodeRepository
                .findByEmailAndCodeAndTypeAndUsedFalseAndExpiredAtAfter(email, code, type, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("验证码无效或已过期"));

        // 标记为已使用
        record.setUsed(true);
        verificationCodeRepository.save(record);
    }

    public UserResponse getCurrentUser(User user) {
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateNickname(User user, String nickname) {
        user.setNickname(nickname);
        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    @Transactional
    public UserResponse updateEmail(User user, String oldEmailCode, String newEmail, String newEmailCode) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("当前账号未绑定邮箱，无需验证旧邮箱");
        }
        // 1. 验证旧邮箱验证码（确认身份）
        verifyCode(user.getEmail(), oldEmailCode, "UPDATE_EMAIL_OLD_CONFIRM");
        // 2. 验证新邮箱未被使用
        if (userRepository.existsByEmail(newEmail) && !newEmail.equals(user.getEmail())) {
            throw new IllegalArgumentException("该邮箱已被其他账号使用");
        }
        // 3. 验证新邮箱验证码
        verifyCode(newEmail, newEmailCode, "UPDATE_EMAIL");
        // 4. 更新邮箱
        user.setEmail(newEmail);
        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    @Transactional
    public void updatePassword(User user, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("新密码不能与旧密码相同");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        verifyCode(email, code, "RESET_PASSWORD");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("该邮箱未注册"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 修改头像：上传到 OSS 的 avatars 目录，返回新头像 URL。
     * 若用户已有旧头像，则删除旧文件。
     */
    @Transactional
    public UserResponse updateAvatar(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择头像文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("仅支持图片格式");
        }
        // 限制 5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("头像大小不能超过5MB");
        }

        // 上传新头像
        S3StorageService.UploadResult uploadResult = s3StorageService.upload(file, "avatars");

        // 删除旧头像文件（best-effort，失败不阻断主流程）
        String oldAvatarKey = user.getAvatarKey();
        if (oldAvatarKey != null && !oldAvatarKey.isBlank()) {
            try {
                s3StorageService.delete(oldAvatarKey);
            } catch (Exception ignored) {
                // 旧头像删除失败不阻断主流程
            }
        }

        user.setAvatar(uploadResult.url());
        user.setAvatarKey(uploadResult.fullKey());
        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getRole(),
                user.getAvatar(),
                user.getCreatedAt()
        );
    }
}