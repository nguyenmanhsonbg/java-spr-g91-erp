package com.g90.backend.modules.contract.entity;

import java.util.Locale;

public enum ContractApprovalStatus {
    NOT_REQUIRED,
    PENDING,
    APPROVED,
    REJECTED,
    MODIFICATION_REQUESTED;

    public static ContractApprovalStatus from(String value) {
        return ContractApprovalStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
