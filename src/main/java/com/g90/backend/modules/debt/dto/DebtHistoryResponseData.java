package com.g90.backend.modules.debt.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DebtHistoryResponseData(
        List<Item> items
) {
    public record Item(
            String eventType,
            String entityId,
            String referenceNo,
            String status,
            BigDecimal amount,
            String description,
            LocalDateTime eventAt
    ) {
    }
}
