package com.g90.backend.modules.inventory.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record InventoryHistoryResponseData(
        List<InventoryHistoryItemResponse> items,
        InventoryPaginationResponse pagination,
        Filters filters
) {
    @Builder
    public record Filters(
            String productId,
            String transactionType,
            LocalDate fromDate,
            LocalDate toDate
    ) {
    }
}
