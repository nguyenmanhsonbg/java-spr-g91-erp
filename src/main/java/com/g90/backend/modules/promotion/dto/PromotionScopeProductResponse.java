package com.g90.backend.modules.promotion.dto;

import lombok.Builder;

@Builder
public record PromotionScopeProductResponse(
        String productId,
        String productCode,
        String productName
) {
}
