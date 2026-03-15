package com.g90.backend.modules.project.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProjectFinancialSummaryResponseData(
        BigDecimal budget,
        BigDecimal actualSpend,
        BigDecimal commitments,
        BigDecimal variance,
        List<CategoryBreakdown> breakdownByCategory,
        BigDecimal paymentsReceived,
        BigDecimal paymentsDue,
        BigDecimal outstandingBalance,
        BigDecimal profitabilityAmount,
        BigDecimal profitabilityMargin,
        String aggregationMode
) {

    public record CategoryBreakdown(
            String category,
            BigDecimal amount
    ) {
    }
}
