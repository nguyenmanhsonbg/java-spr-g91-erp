package com.g90.backend.modules.customer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerSummaryResponseData(
        String customerId,
        String customerCode,
        String companyName,
        String status,
        BigDecimal creditLimit,
        String paymentTerms,
        BigDecimal totalInvoicedAmount,
        BigDecimal totalPaymentsReceived,
        BigDecimal totalAllocatedPayments,
        BigDecimal outstandingDebt,
        long quotationCount,
        long contractCount,
        long invoiceCount,
        long projectCount,
        long activeProjectCount,
        long openContractCount,
        boolean canDisable,
        List<String> disableBlockers,
        LocalDateTime lastTransactionAt
) {
}
