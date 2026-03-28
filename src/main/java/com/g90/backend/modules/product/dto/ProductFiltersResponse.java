package com.g90.backend.modules.product.dto;

import lombok.Builder;

@Builder
public record ProductFiltersResponse(
        String keyword,
        String search,
        String type,
        String size,
        String thickness,
        String unit,
        String status
) {
}
