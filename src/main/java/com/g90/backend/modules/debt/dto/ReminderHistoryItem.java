package com.g90.backend.modules.debt.dto;

import java.time.LocalDateTime;

public record ReminderHistoryItem(
        String id,
        String customerId,
        String customerCode,
        String customerName,
        String invoiceId,
        String invoiceNumber,
        String reminderType,
        String channel,
        String content,
        String status,
        String sentBy,
        LocalDateTime sentAt
) {
}
