package com.g90.backend.modules.quotation.dto;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record QuotationItemResponse(
        String id,
        String productId,
        String productCode,
        String productName,
        String type,
        String size,
        String thickness,
        String unit,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
