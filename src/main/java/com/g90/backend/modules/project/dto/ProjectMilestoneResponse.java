package com.g90.backend.modules.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectMilestoneResponse(
        String id,
        String name,
        String milestoneType,
        Integer completionPercent,
        BigDecimal amount,
        LocalDate dueDate,
        String status,
        String confirmationStatus,
        Boolean confirmed,
        LocalDateTime confirmedAt,
        LocalDateTime confirmationDeadline,
        Boolean autoConfirmEligible,
        Boolean paymentReleaseReady,
        String notes
) {
}
