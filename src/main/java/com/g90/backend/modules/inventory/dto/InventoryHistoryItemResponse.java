package com.g90.backend.modules.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record InventoryHistoryItemResponse(
        String transactionId,
        String transactionCode,
        String transactionType,
        String productId,
        String productCode,
        String productName,
        BigDecimal quantity,
        BigDecimal quantityBefore,
        BigDecimal quantityAfter,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime transactionDate,
        String operatorId,
        String operatorEmail,
        String supplierName,
        String relatedOrderId,
        String relatedProjectId,
        String reason,
        String note
) {
}
