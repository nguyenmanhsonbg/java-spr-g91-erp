package com.g90.backend.modules.quotation.entity;

import java.util.Locale;

public enum QuotationStatus {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED;

    public static QuotationStatus from(String value) {
        return QuotationStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
