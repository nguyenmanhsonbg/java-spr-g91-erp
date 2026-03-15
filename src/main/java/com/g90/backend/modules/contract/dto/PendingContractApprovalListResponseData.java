package com.g90.backend.modules.contract.dto;

import com.g90.backend.modules.product.dto.PaginationResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PendingContractApprovalListResponseData(
        List<Item> items,
        PaginationResponse pagination,
        Filters filters,
        long totalPending
) {
    public record Item(
            String contractId,
            String contractNumber,
            String customerId,
            String customerName,
            BigDecimal totalAmount,
            String approvalStatus,
            String approvalTier,
            String pendingAction,
            String requestedBy,
            LocalDateTime requestedAt,
            LocalDateTime dueAt,
            LocalDateTime submittedAt
    ) {
    }

    public record Filters(
            String keyword,
            String customerId,
            String pendingAction,
            String approvalTier,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo
    ) {
    }
}
