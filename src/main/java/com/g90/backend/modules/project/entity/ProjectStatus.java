package com.g90.backend.modules.project.entity;

import java.util.Locale;

public enum ProjectStatus {
    DRAFT,
    ACTIVE,
    ON_HOLD,
    COMPLETED,
    CLOSED,
    ARCHIVED;

    public static ProjectStatus from(String value) {
        return ProjectStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
