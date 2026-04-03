package com.g90.backend.modules.debt.dto;

import com.g90.backend.modules.product.dto.PaginationResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DebtListResponseData(
        List<Item> items,
        PaginationResponse pagination,
        Filters filters
) {
    public record Item(
            String customerId,
            String customerCode,
            String customerName,
            BigDecimal creditLimit,
            BigDecimal outstandingAmount,
            BigDecimal overdueAmount,
            BigDecimal currentBucket,
            BigDecimal bucket30,
            BigDecimal bucket60,
            BigDecimal bucket90Plus,
            LocalDate lastPaymentDate,
            String status
    ) {
    }

    public record Filters(
            String keyword,
            String customerCode,
            String customerName,
            String invoiceNumber,
            String status,
            Boolean overdueOnly
    ) {
    }
}
