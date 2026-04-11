package com.g90.backend.modules.quotation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.g90.backend.modules.payment.dto.PaymentOptionData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record QuotationPreviewByIdResponseData(
        QuotationData quotation,
        List<QuotationItemResponse> items,
        QuotationPreviewResponseData.SummaryData summary
) {

    public record QuotationData(
            String id,
            String quotationNumber,
            String status,
            LocalDateTime createdAt,
            LocalDate validUntil,
            ProjectData project,
            @JsonProperty("deliveryRequirements") String deliveryRequirements,
            PromotionData promotion,
            PaymentOptionData paymentOption
    ) {
    }

    public record ProjectData(
            String id,
            String projectCode,
            String name
    ) {
    }

    public record PromotionData(
            String code,
            String name
    ) {
    }
}
