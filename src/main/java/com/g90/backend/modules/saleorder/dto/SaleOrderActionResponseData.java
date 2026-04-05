package com.g90.backend.modules.saleorder.dto;

import java.time.LocalDateTime;

public record SaleOrderActionResponseData(
        String saleOrderId,
        String saleOrderNumber,
        String contractNumber,
        String previousStatus,
        String currentStatus,
        String approvalStatus,
        String decision,
        String actedBy,
        LocalDateTime actedAt,
        String note,
        String trackingNumber
) {
}
