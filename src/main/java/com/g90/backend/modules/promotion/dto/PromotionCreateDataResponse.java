package com.g90.backend.modules.promotion.dto;

import lombok.Builder;

@Builder
public record PromotionCreateDataResponse(
        String id,
        String code
) {
}
