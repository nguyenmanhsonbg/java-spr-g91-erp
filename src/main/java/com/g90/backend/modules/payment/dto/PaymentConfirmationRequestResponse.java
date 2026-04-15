package com.g90.backend.modules.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentConfirmationRequestResponse(
        String id,
        String invoiceId,
        String invoiceNumber,
        String customerId,
        String customerCode,
        String customerName,
        BigDecimal requestedAmount,
        BigDecimal confirmedAmount,
        LocalDateTime transferTime,
        String senderBankName,
        String senderAccountName,
        String senderAccountNo,
        String referenceCode,
        String proofDocumentUrl,
        String note,
        String status,
        String reviewNote,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String paymentId,
        BigDecimal invoiceGrandTotal,
        BigDecimal invoicePaidAmount,
        BigDecimal invoiceOutstandingAmount,
        String invoiceStatus
) {
}
