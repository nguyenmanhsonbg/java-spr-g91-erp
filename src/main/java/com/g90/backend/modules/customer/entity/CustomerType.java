package com.g90.backend.modules.customer.entity;

import java.util.Locale;

public enum CustomerType {
    RETAIL,
    CONTRACTOR,
    DISTRIBUTOR;

    public static CustomerType from(String value) {
        return CustomerType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
