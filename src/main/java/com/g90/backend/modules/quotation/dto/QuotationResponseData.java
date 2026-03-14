package com.g90.backend.modules.quotation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record QuotationResponseData(
        String id,
        String quotationNumber,
        String customerId,
        String projectId,
        String status,
        LocalDate validUntil,
        BigDecimal totalAmount,
        String note,
        String deliveryRequirement,
        List<QuotationItemResponse> items,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt
) {
}
