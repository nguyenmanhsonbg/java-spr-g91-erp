package com.g90.backend.modules.contract.dto;

import java.math.BigDecimal;

public record ContractItemResponse(
        String id,
        String productId,
        String productCode,
        String productName,
        String type,
        String size,
        String thickness,
        String unit,
        BigDecimal quantity,
        BigDecimal baseUnitPrice,
        BigDecimal unitPrice,
        BigDecimal discountAmount,
        BigDecimal totalPrice,
        String priceOverrideReason
) {
}
