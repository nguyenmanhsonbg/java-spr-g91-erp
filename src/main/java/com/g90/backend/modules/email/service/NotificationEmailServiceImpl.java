package com.g90.backend.modules.email.service;

import com.g90.backend.modules.email.config.EmailProperties;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

@Service
@RequiredArgsConstructor
public class NotificationEmailServiceImpl implements NotificationEmailService {

    private static final String REGISTRATION_VERIFICATION_TEMPLATE = "email/registration-verification";
    private static final String PASSWORD_RESET_TEMPLATE = "email/forgot-password";
    private static final String CONTRACT_DOCUMENT_TEMPLATE = "email/contract-document";
    private static final String CONTRACT_CREATED_TEMPLATE = "email/contract-created";
    private static final String CONTRACT_APPROVED_TEMPLATE = "email/contract-approved";
    private static final String PROMOTION_CREATED_TEMPLATE = "email/promotion-created";

    private final EmailService emailService;
    private final EmailProperties emailProperties;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public void sendRegistrationVerificationEmail(
            String to,
            String recipientName,
            String verificationCode,
            int expireMinutes
    ) {
        Map<String, Object> variables = baseVariables(recipientName);
        variables.put("verificationCode", verificationCode);
        variables.put("expireMinutes", expireMinutes);

        emailService.sendHtmlEmail(
                to,
                resolveCompanyName() + " registration verification",
                REGISTRATION_VERIFICATION_TEMPLATE,
                variables
        );
    }

    @Override
    public void sendPasswordResetEmail(String to, String recipientName, PasswordResetEmailPayload payload) {
        Map<String, Object> variables = baseVariables(recipientName);
        variables.put("resetToken", payload.resetToken());
        variables.put("expireMinutes", payload.expireMinutes());
        variables.put("resetPasswordUrl", buildResetPasswordUrl(payload.resetToken()));

        emailService.sendHtmlEmail(
                to,
                resolveCompanyName() + " password reset",
                PASSWORD_RESET_TEMPLATE,
                variables
        );
    }

    @Override
    public void sendContractDocumentEmail(String to, String recipientName, ContractDocumentEmailPayload payload) {
        Map<String, Object> variables = baseVariables(recipientName);
        variables.put("contractNumber", payload.contractNumber());
        variables.put("customerName", payload.customerName());
        variables.put("documentType", payload.documentType());
        variables.put("documentNumber", payload.documentNumber());
        variables.put("fileName", payload.fileName());
        variables.put("documentUrl", payload.documentUrl());

        emailService.sendHtmlEmail(
                to,
                resolveCompanyName() + " contract document " + payload.contractNumber(),
                CONTRACT_DOCUMENT_TEMPLATE,
                variables
        );
    }

    @Override
    public void sendContractCreatedEmail(String to, String recipientName, ContractCreatedEmailPayload payload) {
        Map<String, Object> variables = baseVariables(recipientName);
        variables.put("contractNumber", payload.contractNumber());
        variables.put("customerName", payload.customerName());
        variables.put("totalAmount", payload.totalAmount());
        variables.put("createdByName", payload.createdByName());
        variables.put("paymentTerms", payload.paymentTerms());
        variables.put("deliveryAddress", payload.deliveryAddress());
        variables.put("note", payload.note());

        emailService.sendHtmlEmail(
                to,
                resolveCompanyName() + " new contract created " + payload.contractNumber(),
                CONTRACT_CREATED_TEMPLATE,
                variables
        );
    }

    @Override
    public void sendContractApprovedEmail(String to, String recipientName, ContractApprovedEmailPayload payload) {
        Map<String, Object> variables = baseVariables(recipientName);
        variables.put("contractNumber", payload.contractNumber());
        variables.put("customerName", payload.customerName());
        variables.put("totalAmount", payload.totalAmount());
        variables.put("approvedByName", payload.approvedByName());
        variables.put("approvedAt", payload.approvedAt());
        variables.put("paymentTerms", payload.paymentTerms());
        variables.put("deliveryAddress", payload.deliveryAddress());
        variables.put("note", payload.note());

        emailService.sendHtmlEmail(
                to,
                resolveCompanyName() + " contract approved " + payload.contractNumber(),
                CONTRACT_APPROVED_TEMPLATE,
                variables
        );
    }

    @Override
    public void sendPromotionCreatedEmail(String to, String recipientName, PromotionCreatedEmailPayload payload) {
        Map<String, Object> variables = baseVariables(recipientName);
        variables.put("promotionCode", payload.promotionCode());
        variables.put("promotionName", payload.promotionName());
        variables.put("promotionType", payload.promotionType());
        variables.put("discountValue", payload.discountValue());
        variables.put("validFrom", payload.validFrom());
        variables.put("validTo", payload.validTo());
        variables.put("status", payload.status());
        variables.put("description", payload.description());
        variables.put(
                "customerGroupScope",
                payload.customerGroups() == null || payload.customerGroups().isEmpty()
                        ? "All customer groups"
                        : String.join(", ", payload.customerGroups())
        );

        emailService.sendHtmlEmail(
                to,
                resolveCompanyName() + " new promotion " + payload.promotionCode(),
                PROMOTION_CREATED_TEMPLATE,
                variables
        );
    }

    private Map<String, Object> baseVariables(String recipientName) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("recipientName", recipientName);
        variables.put("companyName", resolveCompanyName());
        variables.put("supportEmail", resolveSupportEmail());
        variables.put("assetsBaseUrl", emailProperties.getAssetsBaseUrl());
        return variables;
    }

    private String buildResetPasswordUrl(String resetToken) {
        if (!StringUtils.hasText(emailProperties.getAssetsBaseUrl())) {
            return null;
        }
        return trimTrailingSlash(emailProperties.getAssetsBaseUrl())
                + "/reset-password?token="
                + UriUtils.encode(resetToken, StandardCharsets.UTF_8);
    }

    private String resolveCompanyName() {
        return StringUtils.hasText(emailProperties.getCompanyName()) ? emailProperties.getCompanyName().trim() : "G90 Steel";
    }

    private String resolveSupportEmail() {
        if (StringUtils.hasText(emailProperties.getSupportEmail())) {
            return emailProperties.getSupportEmail().trim();
        }
        return fromEmail;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
