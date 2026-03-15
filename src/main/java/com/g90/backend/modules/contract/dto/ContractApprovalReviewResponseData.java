package com.g90.backend.modules.contract.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ContractApprovalReviewResponseData(
        ContractDetailResponseData detail,
        ApprovalRequestData approvalRequest,
        ReviewInsights insights
) {
    public record ApprovalRequestData(
            String approvalId,
            String approvalType,
            String approvalTier,
            String status,
            String pendingAction,
            String requestedBy,
            LocalDateTime requestedAt,
            LocalDateTime dueAt,
            String comment
    ) {
    }

    public record ReviewInsights(
            List<String> approvalReasons,
            Integer customerHistoryMonths,
            String creditRiskLevel,
            BigDecimal priceChangePercent,
            BigDecimal creditLimit,
            BigDecimal currentDebt,
            BigDecimal projectedDebt,
            BigDecimal availableCredit,
            String profitabilityNote,
            String actionRecommendation
    ) {
    }
}
