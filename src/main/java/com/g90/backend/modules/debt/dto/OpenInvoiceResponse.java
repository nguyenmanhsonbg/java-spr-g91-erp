package com.g90.backend.modules.debt.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OpenInvoiceResponse(
        String id,
        String invoiceNumber,
        LocalDateTime invoiceDate,
        LocalDate dueDate,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        String status,
        long overdueDays
) {
}
