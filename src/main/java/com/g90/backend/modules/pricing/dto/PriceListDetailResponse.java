package com.g90.backend.modules.pricing.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record PriceListDetailResponse(
        String id,
        String name,
        String customerGroup,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        List<PriceListItemResponse> items
) {
}
