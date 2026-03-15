package com.g90.backend.modules.contract.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ContractTrackingResponseData(
        String contractId,
        String contractNumber,
        String currentStatus,
        LocalDateTime lastTrackingRefreshAt,
        int refreshExpectationHours,
        List<Event> events
) {
    public record Event(
            String eventType,
            String eventStatus,
            String title,
            String note,
            LocalDateTime expectedAt,
            LocalDateTime actualAt,
            String trackingNumber
    ) {
    }
}
