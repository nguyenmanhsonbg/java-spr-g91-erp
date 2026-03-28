package com.g90.backend.modules.promotion.entity;

import java.util.Locale;

public enum PromotionStatus {
    DRAFT,
    ACTIVE,
    INACTIVE;

    public static PromotionStatus from(String value) {
        return PromotionStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
