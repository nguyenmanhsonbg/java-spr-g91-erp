package com.g90.backend.modules.saleorder.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SaleOrderTimelineResponseData(
        String saleOrderId,
        String saleOrderNumber,
        String contractNumber,
        String currentStatus,
        LocalDateTime lastTrackingRefreshAt,
        List<Milestone> milestones,
        List<Event> events
) {
    public record Milestone(
            String status,
            String title,
            boolean reached,
            LocalDateTime reachedAt
    ) {
    }

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
