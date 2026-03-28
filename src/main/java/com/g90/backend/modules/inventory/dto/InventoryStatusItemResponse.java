package com.g90.backend.modules.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record InventoryStatusItemResponse(
        String productId,
        String productCode,
        String productName,
        String type,
        String unit,
        BigDecimal currentQuantity,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime updatedAt
) {
}
