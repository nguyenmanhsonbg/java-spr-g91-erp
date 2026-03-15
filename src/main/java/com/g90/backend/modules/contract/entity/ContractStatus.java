package com.g90.backend.modules.contract.entity;

import java.util.Locale;

public enum ContractStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    SUBMITTED,
    PROCESSING,
    RESERVED,
    PICKED,
    IN_TRANSIT,
    DELIVERED,
    COMPLETED,
    CANCELLED;

    public static ContractStatus from(String value) {
        return ContractStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
