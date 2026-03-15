package com.g90.backend.modules.quotation.dto;

import com.g90.backend.modules.product.dto.PaginationResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record QuotationManagementListResponseData(
        List<Item> items,
        PaginationResponse pagination,
        Filters filters
) {
    public record Item(
            String id,
            String quotationNumber,
            String customerId,
            String customerName,
            BigDecimal totalAmount,
            String status,
            LocalDate validUntil,
            LocalDateTime createdAt,
            boolean canEdit,
            boolean canCreateContract
    ) {
    }

    public record Filters(
            String quotationNumber,
            String customerId,
            String status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
    }
}
