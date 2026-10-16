package com.moneykeeper.service;

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Random;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.moneykeeper.entity.VerificationCode;
import com.moneykeeper.repository.VerificationCodeRepository;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final VerificationCodeRepository verificationCodeRepository;

    /** 注入自身代理，保证 @Transactional(REQUIRES_NEW) 同类内调用时生效 */
    @Autowired
    @Lazy
    private EmailService self;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender, VerificationCodeRepository verificationCodeRepository) {
        this.mailSender = mailSender;
        this.verificationCodeRepository = verificationCodeRepository;
    }

    /**
     * 生成6位随机数字验证码
     */
    private String generateCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    /**
     * 发送邮箱验证码
     *
     * 拆分为两步以避免"邮件已发送但事务回滚"或"发送超时但客户端报错却收到了码"的不一致：
     *  1) 先校验邮箱发送额度，再在独立事务中将旧码置为已使用、写入新码并提交（{@link #saveCode}）
     *  2) 再异步发送邮件；失败时立即在独立事务中作废刚写入的验证码
     *
     * 这样能保证：用户能看到的验证码必然在 DB 中存在；发送失败则码立即失效，杜绝"上一次失败的验证码仍能登录"。
     */
    public void sendVerificationCode(String email, String type) {
        checkEmailSendLimit(email);
        String code = generateCode();
        // 1) 先在独立事务中入库并提交，保证后续异步线程也能读到
        self.saveCode(email, type, code);
        // 2) 异步发送邮件，HTTP 接口立即返回，不阻塞用户
        self.asyncSendMail(email, type, code);
    }

    private void checkEmailSendLimit(String email) {
        LocalDateTime now = LocalDateTime.now();
        if (verificationCodeRepository.countByEmailIgnoreCaseAndCreatedAtAfter(email, now.minusMinutes(30)) >= 5) {
            throw new IllegalArgumentException("邮箱验证码发送额度受限，半小时内最多发送5次，请稍后再试");
        }
        if (verificationCodeRepository.countByEmailIgnoreCaseAndCreatedAtAfter(email, now.toLocalDate().atStartOfDay()) >= 8) {
            throw new IllegalArgumentException("邮箱验证码发送额度受限，每日最多发送8次，请明天再试");
        }
    }

    /**
     * 独立事务保存验证码（避免外层有事务时延迟提交）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long saveCode(String email, String type, String code) {
        verificationCodeRepository.markUnusedCodesUsed(email, type);

        LocalDateTime now = LocalDateTime.now();
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setType(type);
        verificationCode.setExpiredAt(now.plusMinutes(5));
        verificationCode.setUsed(false);
        verificationCodeRepository.save(verificationCode);
        return verificationCode.getId();
    }

    /**
     * 独立事务作废发送失败的验证码（按 email + type 将未使用码置为已使用）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidateCode(String email, String type) {
        verificationCodeRepository.markUnusedCodesUsed(email, type);
    }

    /**
     * 异步发送邮件：失败则删除已入库的验证码，杜绝"发送失败但码仍可用"
     */
    @Async
    public void asyncSendMail(String email, String type, String code) {
        try {
            doSendMail(email, type, code);
        } catch (Exception e) {
            try {
                self.invalidateCode(email, type);
            } catch (Exception ignored) {
            }
        }
    }

    private void doSendMail(String email, String type, String code) {
        String subject;
        String content;
        if ("REGISTER".equals(type)) {
            subject = "拾光账本 - 注册验证码";
            content = "您好！\n\n您正在注册拾光账本，验证码为：" + code + "\n\n验证码有效期为5分钟，请尽快完成注册。\n\n如果非本人操作，请忽略此邮件。";
        } else if ("LOGIN".equals(type)) {
            subject = "拾光账本 - 登录验证码";
            content = "您好！\n\n您正在使用邮箱登录拾光账本，验证码为：" + code + "\n\n验证码有效期为5分钟，请尽快完成登录。\n\n如果非本人操作，请忽略此邮件。";
        } else if ("UPDATE_EMAIL".equals(type)) {
            subject = "拾光账本 - 修改邮箱验证码";
            content = "您好！\n\n您正在修改拾光账本的绑定邮箱，验证码为：" + code + "\n\n验证码有效期为5分钟，请尽快完成修改。\n\n如果非本人操作，请忽略此邮件。";
        } else if ("UPDATE_EMAIL_OLD_CONFIRM".equals(type)) {
            subject = "拾光账本 - 修改邮箱身份确认";
            content = "您好！\n\n您正在修改拾光账本的绑定邮箱，此验证码用于确认您的身份，验证码为：" + code + "\n\n验证码有效期为5分钟。\n\n如果非本人操作，请立即忽略此邮件并检查账号安全。";
        } else if ("RESET_PASSWORD".equals(type)) {
            subject = "拾光账本 - 找回密码验证码";
            content = "您好！\n\n您正在找回拾光账本的登录密码，验证码为：" + code + "\n\n验证码有效期为5分钟，请尽快完成密码重置。\n\n如果非本人操作，请忽略此邮件。";
        } else {
            subject = "拾光账本 - 验证码";
            content = "您好！\n\n您的验证码为：" + code + "\n\n验证码有效期为5分钟。\n\n如果非本人操作，请忽略此邮件。";
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("admin <" + fromEmail + ">");
        message.setTo(email);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }

    public void sendTransactionExport(String email, String nickname, Path attachment) throws MessagingException, UnsupportedEncodingException {
        String displayName = nickname == null || nickname.isBlank() ? "用户" : nickname;
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail, "admin");
        helper.setTo(email);
        helper.setSubject("拾光账本 - 账单流水导出完成");
        helper.setText("您好，" + displayName + "！\n\n您提交的账单流水已导出完成，文件见附件。\n\n如非本人操作，请及时检查账号安全。", false);
        helper.addAttachment(attachment.getFileName().toString(), attachment.toFile());
        mailSender.send(message);
    }
}