package com.g90.backend.modules.contract.dto;

import com.g90.backend.modules.product.dto.PaginationResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ContractListResponseData(
        List<Item> items,
        PaginationResponse pagination,
        Filters filters
) {
    public record Item(
            String id,
            String contractNumber,
            String customerId,
            String customerName,
            String status,
            String approvalStatus,
            boolean confidential,
            BigDecimal totalAmount,
            LocalDate expectedDeliveryDate,
            LocalDateTime submittedAt,
            LocalDateTime createdAt
    ) {
    }

    public record Filters(
            String contractNumber,
            String customerId,
            String status,
            String approvalStatus,
            LocalDate createdFrom,
            LocalDate createdTo,
            LocalDate deliveryFrom,
            LocalDate deliveryTo,
            Boolean confidential,
            Boolean submitted
    ) {
    }
}
