package com.g90.backend.modules.email.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {

    private boolean enabled = true;
    private String assetsBaseUrl = "";
    private String resetPasswordUrlTemplate = "";
    private String companyName = "G90 Steel";
    private String supportEmail;
    private Verification verification = new Verification();

    @Getter
    @Setter
    public static class Verification {

        private int expireMinutes = 10;
        private int resendCooldownSeconds = 60;
    }
}
