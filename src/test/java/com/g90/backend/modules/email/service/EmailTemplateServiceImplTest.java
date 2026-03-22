package com.g90.backend.modules.email.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class EmailTemplateServiceImplTest {

    @Test
    void renderRegistrationVerificationTemplate() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        EmailTemplateService emailTemplateService = new EmailTemplateServiceImpl(templateEngine);

        String rendered = emailTemplateService.render("email/registration-verification", Map.of(
                "recipientName", "Nguyen Van A",
                "verificationCode", "A2B3C",
                "expireMinutes", 10,
                "companyName", "G90 Steel",
                "supportEmail", "support@g90steel.vn",
                "assetsBaseUrl", "https://g90steel.vn"
        ));

        assertThat(rendered)
                .contains("Nguyen Van A")
                .contains("A2B3C")
                .contains("10")
                .contains("support@g90steel.vn");
    }
}
