package com.g90.backend.modules.pricing.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record PriceListListResponseData(
        List<PriceListListItemResponse> content,
        int page,
        int size,
        long totalElements
) {
}
