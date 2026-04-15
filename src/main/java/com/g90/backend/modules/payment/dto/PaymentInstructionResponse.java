package com.g90.backend.modules.payment.dto;

import java.math.BigDecimal;

public record PaymentInstructionResponse(
        String paymentMethod,
        String invoiceId,
        String invoiceNumber,
        String customerId,
        BigDecimal grandTotal,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        String bankName,
        String bankAccountName,
        String bankAccountNo,
        String transferContent,
        String qrContent,
        String qrImageUrl
) {
}
