package com.g90.backend.modules.debt.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PaymentExecutionCommand(
        String customerId,
        LocalDate paymentDate,
        BigDecimal amount,
        String paymentMethod,
        String referenceNo,
        String note,
        String proofDocumentUrl,
        String status,
        String actorUserId,
        List<PaymentExecutionAllocation> allocations
) {
    public record PaymentExecutionAllocation(
            String invoiceId,
            BigDecimal amount
    ) {
    }
}
