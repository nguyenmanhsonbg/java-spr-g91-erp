package com.g90.backend.modules.pricing.dto;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record PriceListItemResponse(
        String id,
        String productId,
        String productCode,
        String productName,
        BigDecimal unitPriceVnd,
        String pricingRuleType,
        String note
) {
}
