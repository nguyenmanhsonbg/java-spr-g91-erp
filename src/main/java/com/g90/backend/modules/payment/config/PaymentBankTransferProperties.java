package com.g90.backend.modules.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.payment.bank-transfer")
public class PaymentBankTransferProperties {

    private String bankName = "G90 Bank";
    private String accountName = "G90 Steel";
    private String accountNumber = "0000000000";
}
