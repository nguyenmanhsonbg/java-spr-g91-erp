package com.g90.backend.modules.quotation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record QuotationPreviewResponseData(
        ProjectData project,
        List<QuotationItemResponse> items,
        SummaryData summary,
        PromotionData promotion,
        @JsonProperty("deliveryRequirements") String deliveryRequirements,
        LocalDate validUntil,
        ValidationData validation
) {

    public record ProjectData(
            String id,
            String projectCode,
            String name
    ) {
    }

    public record SummaryData(
            BigDecimal subTotal,
            BigDecimal discountAmount,
            BigDecimal totalAmount
    ) {
    }

    public record PromotionData(
            String code,
            String name,
            Boolean applied
    ) {
    }

    public record ValidationData(
            boolean valid,
            List<String> messages
    ) {
    }
}
