package com.g90.backend.modules.pricing.entity;

import java.util.Locale;

public enum PriceListStatus {
    ACTIVE,
    INACTIVE;

    public static PriceListStatus from(String value) {
        return PriceListStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
