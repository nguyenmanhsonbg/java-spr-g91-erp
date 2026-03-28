package com.g90.backend.modules.promotion.entity;

import java.util.Locale;

public enum PromotionType {
    PERCENT,
    FIXED_AMOUNT;

    public static PromotionType from(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PERCENT", "PERCENTAGE" -> PERCENT;
            case "FIXED", "FIXED_AMOUNT", "AMOUNT" -> FIXED_AMOUNT;
            default -> throw new IllegalArgumentException("Unsupported promotion type");
        };
    }
}
