package com.g90.backend.modules.project.dto;

import java.time.LocalDateTime;

public record ProjectProgressResponse(
        String id,
        String projectId,
        Integer previousProgressPercent,
        Integer progressPercent,
        String progressStatus,
        String phase,
        String notes,
        String changeReason,
        Boolean behindSchedule,
        Integer evidenceCount,
        LocalDateTime createdAt
) {
}
