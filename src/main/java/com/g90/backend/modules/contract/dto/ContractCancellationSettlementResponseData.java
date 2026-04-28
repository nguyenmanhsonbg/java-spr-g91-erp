package com.g90.backend.modules.contract.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContractCancellationSettlementResponseData(
        String id,
        String contractId,
        String contractNumber,
        String customerId,
        String customerCode,
        String customerName,
        String settlementType,
        BigDecimal depositRefundAmount,
        BigDecimal compensationAmount,
        BigDecimal forfeitedDepositAmount,
        BigDecimal totalPayableAmount,
        BigDecimal paidAmount,
        String status,
        String paymentMethod,
        String referenceNo,
        String proofDocumentUrl,
        String note,
        String paymentNote,
        String createdBy,
        String updatedBy,
        String paidBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime paidAt
) {
}
