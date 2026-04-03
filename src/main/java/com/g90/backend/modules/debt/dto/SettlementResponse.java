package com.g90.backend.modules.debt.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SettlementResponse(
        String id,
        String customerId,
        String customerCode,
        String customerName,
        LocalDate settlementDate,
        String confirmedBy,
        String note,
        String certificateUrl,
        String status,
        LocalDateTime createdAt
) {
}
