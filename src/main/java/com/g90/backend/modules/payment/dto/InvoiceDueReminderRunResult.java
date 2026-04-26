package com.g90.backend.modules.payment.dto;

import java.time.LocalDate;

public record InvoiceDueReminderRunResult(
        LocalDate fromDate,
        LocalDate toDate,
        int candidateInvoiceCount,
        int sentEmailCount,
        int sentReminderCount,
        int skippedInvoiceCount
) {
}
