package com.g90.backend.modules.debt.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReminderSendResponseData(
        String customerId,
        String customerCode,
        String customerName,
        String reminderType,
        String channel,
        int sentCount,
        LocalDateTime sentAt,
        List<ReminderHistoryItem> items
) {
}
