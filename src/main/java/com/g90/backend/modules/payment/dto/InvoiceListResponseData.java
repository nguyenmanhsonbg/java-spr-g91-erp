package com.g90.backend.modules.payment.dto;

import com.g90.backend.modules.product.dto.PaginationResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceListResponseData(
        List<Item> items,
        PaginationResponse pagination,
        Filters filters
) {

    public record Item(
            String id,
            String invoiceNumber,
            String sourceType,
            String contractId,
            String contractNumber,
            String customerId,
            String customerCode,
            String customerName,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal grandTotal,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,
            String status,
            String documentUrl
    ) {
    }

    public record Filters(
            String keyword,
            String invoiceNumber,
            String customerId,
            String customerName,
            String contractId,
            String status
    ) {
    }
}
