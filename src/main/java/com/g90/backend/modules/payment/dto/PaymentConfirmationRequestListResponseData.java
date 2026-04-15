package com.g90.backend.modules.payment.dto;

import com.g90.backend.modules.product.dto.PaginationResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentConfirmationRequestListResponseData(
        List<Item> items,
        PaginationResponse pagination,
        Filters filters
) {

    public record Item(
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
            String status,
            String reviewNote,
            String reviewedBy,
            LocalDateTime reviewedAt,
            String paymentId,
            LocalDateTime createdAt
    ) {
    }

    public record Filters(
            String keyword,
            String invoiceId,
            String customerId,
            String status
    ) {
    }
}
