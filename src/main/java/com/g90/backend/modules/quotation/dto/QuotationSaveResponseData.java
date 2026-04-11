package com.g90.backend.modules.quotation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.g90.backend.modules.payment.dto.PaymentOptionData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record QuotationSaveResponseData(
        QuotationData quotation,
        List<QuotationItemResponse> items,
        MetadataData metadata
) {

    public record QuotationData(
            String id,
            String quotationNumber,
            String customerId,
            String projectId,
            BigDecimal totalAmount,
            String status,
            LocalDate validUntil,
            LocalDateTime createdAt
    ) {
    }

    public record MetadataData(
            @JsonProperty("deliveryRequirements") String deliveryRequirements,
            String promotionCode,
            PaymentOptionData paymentOption
    ) {
    }
}
