package com.g90.backend.modules.email.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.g90.backend.modules.email.config.EmailProperties;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEmailServiceImplTest {

    @Mock
    private EmailService emailService;

    @Test
    void sendPasswordResetEmailUsesConfiguredUrlTemplate() {
        EmailProperties properties = new EmailProperties();
        properties.setCompanyName("G90 Steel");
        properties.setSupportEmail("support@g90steel.vn");
        properties.setResetPasswordUrlTemplate("https://app.g90steel.vn/auth/reset-password?token={token}");

        when(emailService.sendHtmlEmail(eq("customer@example.com"), eq("G90 Steel password reset"), eq("email/forgot-password"), anyMap()))
                .thenReturn(CompletableFuture.completedFuture(null));

        NotificationEmailServiceImpl service = new NotificationEmailServiceImpl(emailService, properties);

        service.sendPasswordResetEmail(
                "customer@example.com",
                "Customer A",
                new NotificationEmailService.PasswordResetEmailPayload("token with space", 30)
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendHtmlEmail(
                eq("customer@example.com"),
                eq("G90 Steel password reset"),
                eq("email/forgot-password"),
                variablesCaptor.capture()
        );

        assertThat(variablesCaptor.getValue().get("resetPasswordUrl"))
                .isEqualTo("https://app.g90steel.vn/auth/reset-password?token=token%20with%20space");
    }
}
