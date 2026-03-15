package com.g90.backend.modules.project.entity;

import java.util.Locale;

public enum ProjectProgressStatus {
    ON_TRACK,
    AT_RISK,
    DELAYED,
    COMPLETED;

    public static ProjectProgressStatus from(String value) {
        return ProjectProgressStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
