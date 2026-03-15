package com.g90.backend.modules.project.entity;

import java.util.Locale;

public enum ProjectWarehouseAssignmentType {
    PRIMARY,
    BACKUP;

    public static ProjectWarehouseAssignmentType from(String value) {
        return ProjectWarehouseAssignmentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
