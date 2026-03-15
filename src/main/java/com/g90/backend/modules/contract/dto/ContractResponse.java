package com.g90.backend.modules.contract.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContractResponse(
        String id,
        String contractNumber,
        String customerId,
        String customerName,
        String quotationId,
        String status,
        String approvalStatus,
        String approvalTier,
        boolean requiresApproval,
        boolean confidential,
        String paymentTerms,
        String deliveryAddress,
        String deliveryTerms,
        LocalDate expectedDeliveryDate,
        String note,
        BigDecimal totalAmount,
        BigDecimal creditLimitSnapshot,
        BigDecimal currentDebtSnapshot,
        BigDecimal depositPercentage,
        BigDecimal depositAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime submittedAt,
        LocalDateTime approvedAt,
        LocalDateTime cancelledAt,
        LocalDateTime autoSubmitDueAt
) {
}
