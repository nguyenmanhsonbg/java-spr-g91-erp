package com.g90.backend.modules.customer.entity;

import java.util.Locale;

public enum CustomerStatus {
    ACTIVE,
    PENDING_VERIFICATION,
    INACTIVE;

    public static CustomerStatus from(String value) {
        return CustomerStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
