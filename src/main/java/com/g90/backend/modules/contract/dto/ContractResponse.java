package com.g90.backend.modules.contract.dto;

import com.g90.backend.modules.payment.dto.PaymentOptionData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContractResponse(
        String id,
        String contractNumber,
        String saleOrderNumber,
        String customerId,
        String customerName,
        String quotationId,
        String status,
        String approvalStatus,
        String approvalTier,
        boolean requiresApproval,
        boolean confidential,
        String paymentTerms,
        PaymentOptionData paymentOption,
        String deliveryAddress,
        String deliveryTerms,
        LocalDate expectedDeliveryDate,
        LocalDate actualDeliveryDate,
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
