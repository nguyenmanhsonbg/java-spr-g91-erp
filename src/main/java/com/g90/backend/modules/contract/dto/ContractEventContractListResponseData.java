package com.g90.backend.modules.contract.dto;

import com.g90.backend.modules.product.dto.PaginationResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ContractEventContractListResponseData(
        List<Item> items,
        PaginationResponse pagination,
        Filters filters
) {
    public record Item(
            String contractId,
            String contractNumber,
            String saleOrderNumber,
            String customerId,
            String customerName,
            String contractStatus,
            String approvalStatus,
            BigDecimal totalAmount,
            LocalDate expectedDeliveryDate,
            LocalDateTime submittedAt,
            String eventType,
            String eventStatus,
            String eventTitle,
            String eventNote,
            LocalDateTime eventAt
    ) {
    }

    public record Filters(
            String eventStatus,
            String eventType,
            String keyword,
            String contractNumber,
            String customerId,
            LocalDate eventFrom,
            LocalDate eventTo
    ) {
    }
}
