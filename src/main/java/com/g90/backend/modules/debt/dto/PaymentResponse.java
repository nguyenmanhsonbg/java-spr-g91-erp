package com.g90.backend.modules.debt.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentResponse(
        String id,
        String receiptNumber,
        String customerId,
        String customerCode,
        String customerName,
        LocalDate paymentDate,
        BigDecimal amount,
        String paymentMethod,
        String referenceNo,
        String note,
        String createdBy,
        LocalDateTime createdAt,
        List<AllocationItem> allocations
) {
    public record AllocationItem(
            String invoiceId,
            String invoiceNumber,
            BigDecimal allocatedAmount,
            BigDecimal invoiceRemainingAmount,
            String invoiceStatus
    ) {
    }
}
