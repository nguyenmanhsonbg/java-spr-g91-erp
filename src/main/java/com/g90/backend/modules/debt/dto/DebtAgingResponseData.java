package com.g90.backend.modules.debt.dto;

import java.math.BigDecimal;

public record DebtAgingResponseData(
        BigDecimal outstandingAmount,
        BigDecimal overdueAmount,
        BigDecimal currentBucket,
        BigDecimal bucket30,
        BigDecimal bucket60,
        BigDecimal bucket90Plus
) {
}
