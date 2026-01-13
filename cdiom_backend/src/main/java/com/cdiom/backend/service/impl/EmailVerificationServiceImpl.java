package com.cdiom.backend.service.impl;

import com.cdiom.backend.service.EmailVerificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${email.verification.code-length:6}")
    private int codeLength;

    @Value("${email.verification.expire-minutes:10}")
    private int expireMinutes;

    // 存储验证码：key=email, value=CodeInfo{code, expireTime}
    private final Map<String, CodeInfo> codeStorage = new ConcurrentHashMap<>();

    // 定时清理过期验证码
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @PostConstruct
    public void init() {
        // 每5分钟清理一次过期验证码
        scheduler.scheduleAtFixedRate(this::cleanExpiredCodes, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public String sendVerificationCode(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("邮箱地址不能为空");
        }

        // 生成验证码
        String code = generateCode();
        long expireTime = System.currentTimeMillis() + expireMinutes * 60 * 1000L;

        // 存储验证码
        codeStorage.put(email, new CodeInfo(code, expireTime));

        // 发送邮件
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(Objects.requireNonNull(fromEmail, "发件人邮箱不能为空"));
            helper.setTo(Objects.requireNonNull(email, "收件人邮箱不能为空"));
            helper.setSubject(Objects.requireNonNull("CDIOM系统 - 超级管理员操作验证码"));
            helper.setText(Objects.requireNonNull(buildEmailContent(code)), true);

            mailSender.send(message);
            log.info("验证码已发送到邮箱: {}", email);

            // 生产环境返回null，开发环境可以返回验证码用于测试
            return null; // 返回null，不暴露验证码
        } catch (MessagingException e) {
            log.error("发送验证码邮件失败，邮箱: {}", email, e);
            codeStorage.remove(email); // 发送失败，清除验证码
            throw new RuntimeException("发送验证码失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyCode(String email, String code) {
        if (email == null || code == null) {
            return false;
        }

        CodeInfo codeInfo = codeStorage.get(email);
        if (codeInfo == null) {
            log.warn("验证码不存在或已过期，邮箱: {}", email);
            return false;
        }

        // 检查是否过期
        if (System.currentTimeMillis() > codeInfo.expireTime) {
            codeStorage.remove(email);
            log.warn("验证码已过期，邮箱: {}", email);
            return false;
        }

        // 验证码验证（不区分大小写）
        boolean isValid = codeInfo.code.equalsIgnoreCase(code);
        if (isValid) {
            log.info("验证码验证成功，邮箱: {}", email);
        } else {
            log.warn("验证码验证失败，邮箱: {}", email);
        }

        return isValid;
    }

    @Override
    public void clearCode(String email) {
        codeStorage.remove(email);
        log.debug("已清除验证码，邮箱: {}", email);
    }

    /**
     * 生成验证码
     */
    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 构建邮件内容
     */
    private String buildEmailContent(String code) {
        return "<html><body style='font-family: Arial, sans-serif;'>" +
                "<h2 style='color: #1890ff;'>CDIOM系统 - 超级管理员操作验证码</h2>" +
                "<p>您正在进行超级管理员启用/停用操作，验证码为：</p>" +
                "<p style='font-size: 24px; font-weight: bold; color: #ff4d4f; letter-spacing: 5px;'>" + code + "</p>" +
                "<p>验证码有效期为 " + expireMinutes + " 分钟，请勿泄露给他人。</p>" +
                "<p style='color: #999; font-size: 12px;'>如非本人操作，请忽略此邮件。</p>" +
                "</body></html>";
    }

    /**
     * 清理过期验证码
     */
    private void cleanExpiredCodes() {
        long now = System.currentTimeMillis();
        codeStorage.entrySet().removeIf(entry -> now > entry.getValue().expireTime);
        log.debug("已清理过期验证码，当前存储数量: {}", codeStorage.size());
    }

    /**
     * 验证码信息
     */
    private static class CodeInfo {
        final String code;
        final long expireTime;

        CodeInfo(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
        }
    }
}

