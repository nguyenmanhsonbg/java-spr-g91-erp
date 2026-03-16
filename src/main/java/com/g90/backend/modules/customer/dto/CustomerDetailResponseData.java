package com.g90.backend.modules.customer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerDetailResponseData(
        CustomerResponse customer,
        PortalAccountData portalAccount,
        FinancialData financial,
        ActivityData activity,
        List<ContactPersonData> contactPersons,
        List<RecentTransactionData> recentTransactions,
        List<DocumentData> documents
) {
    public record PortalAccountData(
            String userId,
            String email,
            String status
    ) {
    }

    public record FinancialData(
            BigDecimal creditLimit,
            String paymentTerms,
            BigDecimal totalInvoicedAmount,
            BigDecimal totalPaymentsReceived,
            BigDecimal totalAllocatedPayments,
            BigDecimal outstandingDebt
    ) {
    }

    public record ActivityData(
            long quotationCount,
            long contractCount,
            long invoiceCount,
            long projectCount,
            long activeProjectCount,
            long openContractCount,
            LocalDateTime lastTransactionAt
    ) {
    }

    public record ContactPersonData(
            String fullName,
            String phone,
            String email,
            boolean primary
    ) {
    }

    public record RecentTransactionData(
            String type,
            String entityId,
            String referenceNo,
            String status,
            BigDecimal amount,
            LocalDateTime eventAt
    ) {
    }

    public record DocumentData(
            String type,
            String fileName,
            String fileUrl,
            LocalDateTime uploadedAt
    ) {
    }
}
