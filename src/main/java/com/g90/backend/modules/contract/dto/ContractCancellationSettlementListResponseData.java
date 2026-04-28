package com.g90.backend.modules.contract.dto;

import com.g90.backend.modules.product.dto.PaginationResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ContractCancellationSettlementListResponseData(
        List<Item> items,
        PaginationResponse pagination,
        Filters filters
) {

    public record Item(
            String id,
            String contractId,
            String contractNumber,
            String customerId,
            String customerCode,
            String customerName,
            String settlementType,
            BigDecimal totalPayableAmount,
            BigDecimal paidAmount,
            String status,
            LocalDateTime createdAt,
            LocalDateTime paidAt
    ) {
    }

    public record Filters(
            String contractId,
            String customerId,
            String settlementType,
            String status
    ) {
    }
}
