package com.g90.backend.modules.payment.service;

import com.g90.backend.modules.payment.dto.InvoiceDueReminderRunResult;

public interface InvoiceDueReminderService {

    InvoiceDueReminderRunResult sendDueInvoiceReminders();
}
