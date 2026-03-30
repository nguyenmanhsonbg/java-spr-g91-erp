package com.g90.backend.modules.email.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationEmailService {

    void sendRegistrationVerificationEmail(String to, String recipientName, String verificationCode, int expireMinutes);

    void sendPasswordResetEmail(String to, String recipientName, PasswordResetEmailPayload payload);

    void sendContractDocumentEmail(String to, String recipientName, ContractDocumentEmailPayload payload);

    void sendContractCreatedEmail(String to, String recipientName, ContractCreatedEmailPayload payload);

    void sendContractApprovedEmail(String to, String recipientName, ContractApprovedEmailPayload payload);

    void sendPromotionCreatedEmail(String to, String recipientName, PromotionCreatedEmailPayload payload);

    record PasswordResetEmailPayload(
            String resetToken,
            int expireMinutes
    ) {
    }

    record ContractDocumentEmailPayload(
            String contractNumber,
            String customerName,
            String documentType,
            String documentNumber,
            String fileName,
            String documentUrl
    ) {
    }

    record ContractCreatedEmailPayload(
            String contractNumber,
            String customerName,
            BigDecimal totalAmount,
            String createdByName,
            String paymentTerms,
            String deliveryAddress,
            String note
    ) {
    }

    record ContractApprovedEmailPayload(
            String contractNumber,
            String customerName,
            BigDecimal totalAmount,
            String approvedByName,
            LocalDateTime approvedAt,
            String paymentTerms,
            String deliveryAddress,
            String note
    ) {
    }

    record PromotionCreatedEmailPayload(
            String promotionCode,
            String promotionName,
            String promotionType,
            BigDecimal discountValue,
            LocalDate validFrom,
            LocalDate validTo,
            String status,
            String description,
            List<String> customerGroups
    ) {
    }
}
