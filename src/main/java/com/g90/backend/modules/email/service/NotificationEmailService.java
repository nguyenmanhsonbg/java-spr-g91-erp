package com.g90.backend.modules.email.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface NotificationEmailService {

    void sendRegistrationVerificationEmail(String to, String recipientName, String verificationCode, int expireMinutes);

    void sendPasswordResetEmail(String to, String recipientName, PasswordResetEmailPayload payload);

    void sendContractDocumentEmail(String to, String recipientName, ContractDocumentEmailPayload payload);

    void sendContractCreatedEmail(String to, String recipientName, ContractCreatedEmailPayload payload);

    void sendContractApprovedEmail(String to, String recipientName, ContractApprovedEmailPayload payload);

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
}
