package com.g90.backend.modules.quotation.dto;

import java.math.BigDecimal;
import java.util.List;

public record QuotationFormInitResponseData(
        CustomerData customer,
        List<ProductData> products,
        List<ProjectData> projects,
        List<PromotionData> availablePromotions
) {

    public record CustomerData(
            String id,
            String companyName,
            String customerType,
            String status
    ) {
    }

    public record ProductData(
            String id,
            String productCode,
            String productName,
            String type,
            String size,
            String thickness,
            String unit,
            BigDecimal referenceWeight,
            String status,
            BigDecimal referenceUnitPrice
    ) {
    }

    public record ProjectData(
            String id,
            String projectCode,
            String name,
            String status
    ) {
    }

    public record PromotionData(
            String code,
            String name,
            String discountType,
            BigDecimal discountValue
    ) {
    }
}
