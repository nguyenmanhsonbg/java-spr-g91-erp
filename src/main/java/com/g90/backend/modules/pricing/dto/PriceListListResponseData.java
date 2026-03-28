package com.g90.backend.modules.pricing.dto;

import com.g90.backend.modules.product.dto.PaginationResponse;
import java.time.LocalDate;
import java.util.List;

public record PriceListListResponseData(
        List<PriceListListItemResponse> items,
        PaginationResponse pagination,
        Filters filters
) {
    public record Filters(
            String search,
            String status,
            String customerGroup,
            LocalDate validFrom,
            LocalDate validTo
    ) {
    }
}
