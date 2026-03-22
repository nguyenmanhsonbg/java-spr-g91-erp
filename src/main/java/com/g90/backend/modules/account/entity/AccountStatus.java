package com.g90.backend.modules.account.entity;

import java.util.Locale;

public enum AccountStatus {
    ACTIVE,
    PENDING_VERIFICATION,
    INACTIVE,
    LOCKED;

    public static AccountStatus from(String value) {
        return AccountStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
