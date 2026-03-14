package com.g90.backend.modules.quotation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QuotationHistoryResponseData(
        String quotationId,
        List<EventData> events
) {

    public record EventData(
            String id,
            String action,
            String actorRole,
            String actorName,
            String note,
            LocalDateTime createdAt
    ) {
    }
}
