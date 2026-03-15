package com.g90.backend.modules.project.entity;

import java.util.Locale;

public enum ProjectMilestoneStatus {
    PENDING,
    READY_FOR_CONFIRMATION,
    CONFIRMED,
    AUTO_CONFIRMED,
    REJECTED,
    COMPLETED;

    public static ProjectMilestoneStatus from(String value) {
        return ProjectMilestoneStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
