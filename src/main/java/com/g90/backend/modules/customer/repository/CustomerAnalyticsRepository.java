package com.g90.backend.modules.customer.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CustomerAnalyticsRepository {

    CustomerAggregateSnapshot getAggregateSnapshot(String customerId);

    List<CustomerRecentTransactionSnapshot> getRecentTransactions(String customerId, int limit);

    record CustomerAggregateSnapshot(
            long quotationCount,
            long contractCount,
            long invoiceCount,
            long projectCount,
            long activeProjectCount,
            long openContractCount,
            BigDecimal totalInvoicedAmount,
            BigDecimal totalPaymentsReceived,
            BigDecimal totalAllocatedPayments,
            LocalDateTime lastTransactionAt
    ) {
    }

    record CustomerRecentTransactionSnapshot(
            String type,
            String entityId,
            String referenceNo,
            String status,
            BigDecimal amount,
            LocalDateTime eventAt
    ) {
    }
}
