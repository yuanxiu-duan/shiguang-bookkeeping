package com.moneykeeper.config;

import com.moneykeeper.entity.User;
import com.moneykeeper.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private static final String ADMIN_USERNAME = "admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password:change-me-admin-password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        userRepository.findByUsername(ADMIN_USERNAME).ifPresentOrElse(
                user -> {
                    if (!"ADMIN".equals(user.getRole())) {
                        user.setRole("ADMIN");
                        userRepository.save(user);
                        log.info("已为用户 {} 补充ADMIN角色", ADMIN_USERNAME);
                    }
                },
                () -> {
                    User admin = new User();
                    admin.setUsername(ADMIN_USERNAME);
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    admin.setNickname("管理员");
                    admin.setRole("ADMIN");
                    userRepository.save(admin);
                    log.info("初始化管理员账号: {}，默认密码: {}", ADMIN_USERNAME, adminPassword);
                }
        );
    }
}
