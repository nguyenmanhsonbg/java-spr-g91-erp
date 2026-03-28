package com.g90.backend.modules.product.service;

import java.util.Locale;
import org.springframework.util.StringUtils;

public enum ProductDeletionMode {
    WAREHOUSE_SOFT_DEACTIVATE,
    OWNER_APPROVAL_SOFT_DEACTIVATE;

    public static ProductDeletionMode fromProperty(String value) {
        if (!StringUtils.hasText(value)) {
            return WAREHOUSE_SOFT_DEACTIVATE;
        }

        try {
            return ProductDeletionMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid app.product.deletion.mode: " + value, exception);
        }
    }
}
