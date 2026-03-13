package com.g90.backend.modules.product.entity;

import java.util.Locale;

public enum ProductStatus {
    ACTIVE,
    INACTIVE;

    public static ProductStatus from(String value) {
        return ProductStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
