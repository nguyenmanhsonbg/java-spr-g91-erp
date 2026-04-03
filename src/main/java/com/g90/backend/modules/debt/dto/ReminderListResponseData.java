package com.g90.backend.modules.debt.dto;

import com.g90.backend.modules.product.dto.PaginationResponse;
import java.util.List;

public record ReminderListResponseData(
        List<ReminderHistoryItem> items,
        PaginationResponse pagination,
        Filters filters
) {
    public record Filters(
            String customerId,
            String invoiceNumber,
            String status,
            String reminderType,
            String channel
    ) {
    }
}
