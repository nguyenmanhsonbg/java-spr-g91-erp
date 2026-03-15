package com.g90.backend.modules.project.entity;

import java.util.Locale;

public enum ProjectApprovalStatus {
    NOT_REQUIRED,
    APPROVAL_READY,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED;

    public static ProjectApprovalStatus from(String value) {
        return ProjectApprovalStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
