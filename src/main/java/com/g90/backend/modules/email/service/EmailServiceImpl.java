package com.g90.backend.modules.email.service;

import com.g90.backend.modules.email.config.EmailProperties;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final EmailTemplateService emailTemplateService;
    private final EmailProperties emailProperties;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendHtmlEmail(
            String to,
            String subject,
            String templateName,
            Map<String, Object> variables
    ) {
        if (!emailProperties.isEnabled()) {
            log.info("Email sending skipped because app.email.enabled=false for recipient={}", to);
            return CompletableFuture.completedFuture(null);
        }

        try {
            log.info("Sending email template={} to={}", templateName, to);
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(emailTemplateService.render(templateName, variables), true);
            javaMailSender.send(mimeMessage);
            log.info("Email sent successfully template={} to={}", templateName, to);
        } catch (Exception exception) {
            log.error("Failed to send email template={} to={}", templateName, to, exception);
        }

        return CompletableFuture.completedFuture(null);
    }
}
