package com.g90.backend.modules.account.entity;

import java.util.Locale;

public enum RoleName {
    OWNER,
    ACCOUNTANT,
    WAREHOUSE,
    CUSTOMER;

    public static RoleName from(String value) {
        return RoleName.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
