package com.g90.backend.modules.inventory.dto;

import lombok.Builder;

@Builder
public record InventoryPaginationResponse(
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
