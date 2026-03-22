package com.g90.backend.modules.email.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.g90.backend.modules.email.config.EmailProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private EmailTemplateService emailTemplateService;

    private EmailProperties emailProperties;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailService = new EmailServiceImpl(javaMailSender, emailTemplateService, emailProperties);
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@g90steel.vn");
    }

    @Test
    void sendHtmlEmailSkipsWhenDisabled() {
        emailProperties.setEnabled(false);

        emailService.sendHtmlEmail("customer@example.com", "Subject", "email/registration-verification", Map.of()).join();

        verify(javaMailSender, never()).createMimeMessage();
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendHtmlEmailUsesJavaMailSenderWhenEnabled() throws Exception {
        emailProperties.setEnabled(true);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailTemplateService.render(eq("email/registration-verification"), anyMap())).thenReturn("<html><body>Verification</body></html>");

        emailService.sendHtmlEmail(
                "customer@example.com",
                "Subject",
                "email/registration-verification",
                Map.of("verificationCode", "A2B3C")
        ).join();

        verify(emailTemplateService).render(eq("email/registration-verification"), anyMap());
        verify(javaMailSender).send(mimeMessage);
    }
}
