package com.g90.backend.modules.inventory.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record InventoryStatusResponseData(
        List<InventoryStatusItemResponse> items,
        InventoryPaginationResponse pagination,
        Filters filters
) {
    @Builder
    public record Filters(
            String search,
            String productId
    ) {
    }
}
