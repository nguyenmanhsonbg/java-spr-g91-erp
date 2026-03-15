package com.g90.backend.modules.contract.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ContractPreviewResponseData(
        String customerId,
        String customerName,
        String quotationId,
        String quotationNumber,
        List<ContractItemResponse> items,
        BigDecimal totalAmount,
        boolean requiresApproval,
        String approvalTier,
        BigDecimal creditLimit,
        BigDecimal currentDebt,
        BigDecimal projectedDebt,
        BigDecimal depositPercentage,
        BigDecimal depositAmount,
        List<String> warnings,
        LocalDate expectedDeliveryDate,
        LocalDateTime autoSubmitDueAt
) {
}
