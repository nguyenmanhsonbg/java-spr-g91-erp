package com.g90.backend.modules.quotation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record QuotationPreviewResponseData(
        String customerId,
        String projectId,
        String status,
        LocalDate validUntil,
        BigDecimal totalAmount,
        String note,
        String deliveryRequirement,
        List<QuotationItemResponse> items
) {
}
