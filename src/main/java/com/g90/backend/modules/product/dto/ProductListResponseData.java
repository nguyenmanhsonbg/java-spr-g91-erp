package com.g90.backend.modules.product.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record ProductListResponseData(
        List<ProductResponse> items,
        PaginationResponse pagination,
        ProductFiltersResponse filters
) {
}
