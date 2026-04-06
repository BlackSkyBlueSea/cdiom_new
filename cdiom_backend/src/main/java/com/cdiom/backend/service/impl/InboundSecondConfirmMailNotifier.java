package com.cdiom.backend.service.impl;

import com.cdiom.backend.model.InboundRecord;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 特殊药品入库待第二人确认时发送邮件提醒（仅提醒，审批须登录系统完成）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InboundSecondConfirmMailNotifier {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void notifySecondOperatorPending(InboundRecord record, String toEmail, String toUsername) {
        if (!StringUtils.hasText(toEmail)) {
            log.debug("第二操作人未配置邮箱，跳过邮件提醒，入库单号={}", record.getRecordNumber());
            return;
        }
        if (!StringUtils.hasText(fromEmail)) {
            log.warn("未配置 spring.mail.username，跳过入库待确认邮件");
            return;
        }
        final String recipientEmail = toEmail.trim();
        final String senderEmail = fromEmail.trim();
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(Objects.requireNonNull(senderEmail));
            helper.setTo(Objects.requireNonNull(recipientEmail));
            helper.setSubject("CDIOM：特殊药品入库待您确认（请登录系统处理）");
            String body = String.format(
                    "<p>您好%s，</p>"
                            + "<p>入库单 <b>%s</b> 已初验合格，需您作为第二操作人登录 CDIOM 系统在「入库管理」中确认后方可入账。</p>"
                            + "<p>请勿仅依赖邮件操作；请在系统内完成确认。</p>"
                            + "<p>若超时未确认，该单将自动关闭，需由仓库重新发起。</p>",
                    StringUtils.hasText(toUsername) ? " " + toUsername : "",
                    record.getRecordNumber() != null ? record.getRecordNumber() : String.valueOf(record.getId()));
            helper.setText(Objects.requireNonNull(body), true);
            mailSender.send(message);
            log.info("已发送特殊药品入库待确认提醒邮件至 {}", recipientEmail);
        } catch (Exception e) {
            log.warn("发送特殊药品入库待确认邮件失败: {}", e.getMessage());
        }
    }
}
