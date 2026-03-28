package com.g90.backend.modules.inventory.entity;

import java.util.Locale;

public enum InventoryTransactionType {
    RECEIPT,
    ISSUE,
    ADJUSTMENT;

    public static InventoryTransactionType from(String value) {
        return InventoryTransactionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
