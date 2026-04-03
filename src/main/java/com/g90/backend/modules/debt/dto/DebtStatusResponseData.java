package com.g90.backend.modules.debt.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DebtStatusResponseData(
        Summary summary,
        DebtAgingResponseData aging,
        List<OpenInvoiceResponse> openInvoices,
        List<PaymentResponse> paymentHistory,
        List<ReminderHistoryItem> reminderHistory,
        List<SettlementResponse> settlements
) {
    public record Summary(
            String customerId,
            String customerCode,
            String customerName,
            BigDecimal creditLimit,
            String paymentTerms,
            BigDecimal totalInvoiceAmount,
            BigDecimal totalPaymentsReceived,
            BigDecimal totalAllocatedPayments,
            BigDecimal outstandingAmount,
            BigDecimal overdueAmount,
            LocalDate lastPaymentDate,
            String status
    ) {
    }
}
