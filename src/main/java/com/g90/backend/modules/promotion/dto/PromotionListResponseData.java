package com.g90.backend.modules.promotion.dto;

import com.g90.backend.modules.product.dto.PaginationResponse;
import java.time.LocalDate;
import java.util.List;

public record PromotionListResponseData(
        List<PromotionListItemResponse> items,
        PaginationResponse pagination,
        Filters filters
) {
    public record Filters(
            String search,
            String status,
            String promotionType,
            LocalDate validFrom,
            LocalDate validTo,
            String customerGroup,
            String productId
    ) {
    }
}
