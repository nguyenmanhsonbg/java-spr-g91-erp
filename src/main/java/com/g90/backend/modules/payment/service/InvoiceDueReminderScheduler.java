package com.g90.backend.modules.payment.service;

import com.g90.backend.modules.payment.dto.InvoiceDueReminderRunResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.invoice.due-reminder", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InvoiceDueReminderScheduler {

    private final InvoiceDueReminderService invoiceDueReminderService;

    @Scheduled(
            cron = "${app.invoice.due-reminder.cron:0 0 8 * * *}",
            zone = "${app.invoice.due-reminder.time-zone:Asia/Ho_Chi_Minh}"
    )
    public void sendDueInvoiceReminders() {
        InvoiceDueReminderRunResult result = invoiceDueReminderService.sendDueInvoiceReminders();
        log.info(
                "Invoice due reminder completed fromDate={} toDate={} candidates={} emails={} reminders={} skipped={}",
                result.fromDate(),
                result.toDate(),
                result.candidateInvoiceCount(),
                result.sentEmailCount(),
                result.sentReminderCount(),
                result.skippedInvoiceCount()
        );
    }
}
