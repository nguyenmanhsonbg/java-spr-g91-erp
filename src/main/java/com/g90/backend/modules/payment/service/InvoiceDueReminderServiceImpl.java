package com.g90.backend.modules.payment.service;

import com.g90.backend.modules.debt.entity.DebtReminderEntity;
import com.g90.backend.modules.debt.repository.DebtInvoiceRepository;
import com.g90.backend.modules.debt.repository.DebtReminderRepository;
import com.g90.backend.modules.debt.repository.PaymentAllocationRepository;
import com.g90.backend.modules.email.config.EmailProperties;
import com.g90.backend.modules.email.service.EmailService;
import com.g90.backend.modules.payment.dto.InvoiceDueReminderRunResult;
import com.g90.backend.modules.payment.entity.InvoiceEntity;
import com.g90.backend.modules.payment.repository.InvoiceRepository;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InvoiceDueReminderServiceImpl implements InvoiceDueReminderService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String REMINDER_TYPE = "DUE";
    private static final String REMINDER_CHANNEL = "EMAIL";
    private static final String SYSTEM_SENDER = "SYSTEM";
    private static final String TEMPLATE = "email/invoice-due-reminder";
    private static final Set<String> EXCLUDED_STATUSES = Set.of("DRAFT", "PAID", "SETTLED", "CLOSED", "CANCELLED", "VOID");

    private final InvoiceRepository invoiceRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final DebtReminderRepository debtReminderRepository;
    private final DebtInvoiceRepository debtInvoiceRepository;
    private final EmailService emailService;
    private final EmailProperties emailProperties;
    private final int lookAheadDays;
    private final String timeZone;

    public InvoiceDueReminderServiceImpl(
            InvoiceRepository invoiceRepository,
            PaymentAllocationRepository paymentAllocationRepository,
            DebtReminderRepository debtReminderRepository,
            DebtInvoiceRepository debtInvoiceRepository,
            EmailService emailService,
            EmailProperties emailProperties,
            @Value("${app.invoice.due-reminder.look-ahead-days:0}") int lookAheadDays,
            @Value("${app.invoice.due-reminder.time-zone:Asia/Ho_Chi_Minh}") String timeZone
    ) {
        this.invoiceRepository = invoiceRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.debtReminderRepository = debtReminderRepository;
        this.debtInvoiceRepository = debtInvoiceRepository;
        this.emailService = emailService;
        this.emailProperties = emailProperties;
        this.lookAheadDays = Math.max(0, lookAheadDays);
        this.timeZone = timeZone;
    }

    @Override
    @Transactional
    public InvoiceDueReminderRunResult sendDueInvoiceReminders() {
        ZoneId zone = resolveZone();
        LocalDate today = LocalDate.now(zone);
        LocalDate toDate = today.plusDays(lookAheadDays);
        List<InvoiceEntity> candidates = invoiceRepository.findDueReminderCandidates(today, toDate, EXCLUDED_STATUSES);
        if (candidates.isEmpty()) {
            return new InvoiceDueReminderRunResult(today, toDate, 0, 0, 0, 0);
        }

        List<DueInvoiceReminderItem> dueInvoices = resolveDueInvoices(candidates, today);
        Map<String, List<DueInvoiceReminderItem>> invoicesByCustomer = groupByCustomer(dueInvoices);

        int sentEmailCount = 0;
        int sentReminderCount = 0;
        int skippedInvoiceCount = candidates.size() - dueInvoices.size();
        for (List<DueInvoiceReminderItem> customerInvoices : invoicesByCustomer.values()) {
            CustomerProfileEntity customer = customerInvoices.get(0).invoice().getCustomer();
            if (!StringUtils.hasText(customer.getEmail())) {
                skippedInvoiceCount += customerInvoices.size();
                continue;
            }

            LocalDateTime sentAt = LocalDateTime.now(zone);
            emailService.sendHtmlEmail(
                    customer.getEmail().trim(),
                    buildSubject(customer, today, toDate),
                    TEMPLATE,
                    buildVariables(customer, customerInvoices, today, toDate, sentAt)
            ).join();
            sentEmailCount++;
            sentReminderCount += saveReminderHistory(customer, customerInvoices, sentAt);
        }

        return new InvoiceDueReminderRunResult(
                today,
                toDate,
                candidates.size(),
                sentEmailCount,
                sentReminderCount,
                skippedInvoiceCount
        );
    }

    private List<DueInvoiceReminderItem> resolveDueInvoices(List<InvoiceEntity> candidates, LocalDate today) {
        List<String> invoiceIds = candidates.stream().map(InvoiceEntity::getId).toList();
        Set<String> alreadyRemindedInvoiceIds = new LinkedHashSet<>(
                debtReminderRepository.findInvoiceIdsByReminderType(invoiceIds, REMINDER_TYPE)
        );
        Map<String, BigDecimal> paidAmountByInvoiceId = loadPaidAmounts(invoiceIds);

        List<DueInvoiceReminderItem> dueInvoices = new ArrayList<>();
        for (InvoiceEntity invoice : candidates) {
            if (invoice.getCustomer() == null || alreadyRemindedInvoiceIds.contains(invoice.getId())) {
                continue;
            }
            BigDecimal grandTotal = grandTotal(invoice);
            BigDecimal paidAmount = paidAmountByInvoiceId.getOrDefault(invoice.getId(), ZERO).min(grandTotal).setScale(2, RoundingMode.HALF_UP);
            BigDecimal outstandingAmount = grandTotal.subtract(paidAmount).max(ZERO).setScale(2, RoundingMode.HALF_UP);
            if (outstandingAmount.compareTo(ZERO) <= 0) {
                continue;
            }
            dueInvoices.add(new DueInvoiceReminderItem(
                    invoice,
                    grandTotal,
                    paidAmount,
                    outstandingAmount,
                    invoice.getDueDate() == null ? 0 : ChronoUnit.DAYS.between(today, invoice.getDueDate()),
                    deriveStatus(invoice, paidAmount, outstandingAmount)
            ));
        }
        return dueInvoices;
    }

    private Map<String, List<DueInvoiceReminderItem>> groupByCustomer(List<DueInvoiceReminderItem> dueInvoices) {
        Map<String, List<DueInvoiceReminderItem>> result = new LinkedHashMap<>();
        for (DueInvoiceReminderItem item : dueInvoices) {
            CustomerProfileEntity customer = item.invoice().getCustomer();
            result.computeIfAbsent(customer.getId(), ignored -> new ArrayList<>()).add(item);
        }
        return result;
    }

    private Map<String, BigDecimal> loadPaidAmounts(Collection<String> invoiceIds) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (PaymentAllocationRepository.InvoiceAllocationTotalView view : paymentAllocationRepository.summarizeByInvoiceIds(invoiceIds)) {
            result.put(view.getInvoiceId(), normalizeMoney(view.getAllocatedAmount()));
        }
        return result;
    }

    private Map<String, Object> buildVariables(
            CustomerProfileEntity customer,
            List<DueInvoiceReminderItem> invoices,
            LocalDate fromDate,
            LocalDate toDate,
            LocalDateTime sentAt
    ) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("recipientName", firstNonBlank(customer.getContactPerson(), customer.getCompanyName(), "Customer"));
        variables.put("customerName", firstNonBlank(customer.getCompanyName(), "Customer"));
        variables.put("companyName", resolveCompanyName());
        variables.put("supportEmail", resolveSupportEmail());
        variables.put("fromDate", fromDate);
        variables.put("toDate", toDate);
        variables.put("sentAt", sentAt);
        variables.put("message", buildMessage(fromDate, toDate));
        variables.put("invoices", invoices.stream().map(this::toEmailInvoiceData).toList());
        variables.put("totalOutstanding", invoices.stream()
                .map(DueInvoiceReminderItem::outstandingAmount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP));
        return variables;
    }

    private Map<String, Object> toEmailInvoiceData(DueInvoiceReminderItem item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("invoiceNumber", item.invoice().getInvoiceNumber());
        payload.put("dueDate", item.invoice().getDueDate());
        payload.put("grandTotal", item.grandTotal());
        payload.put("paidAmount", item.paidAmount());
        payload.put("outstandingAmount", item.outstandingAmount());
        payload.put("daysUntilDue", item.daysUntilDue());
        payload.put("status", item.status());
        return payload;
    }

    private int saveReminderHistory(CustomerProfileEntity customer, List<DueInvoiceReminderItem> invoices, LocalDateTime sentAt) {
        String content = buildReminderContent(customer, invoices);
        List<DebtReminderEntity> reminders = new ArrayList<>();
        for (DueInvoiceReminderItem item : invoices) {
            DebtReminderEntity reminder = new DebtReminderEntity();
            reminder.setCustomerId(customer.getId());
            reminder.setInvoice(debtInvoiceRepository.getReferenceById(item.invoice().getId()));
            reminder.setReminderType(REMINDER_TYPE);
            reminder.setChannel(REMINDER_CHANNEL);
            reminder.setContent(content);
            reminder.setNote("Automatic due invoice reminder");
            reminder.setSentBy(SYSTEM_SENDER);
            reminder.setSentAt(sentAt);
            reminder.setStatus("SENT");
            reminders.add(reminder);
        }
        return debtReminderRepository.saveAll(reminders).size();
    }

    private String buildReminderContent(CustomerProfileEntity customer, List<DueInvoiceReminderItem> invoices) {
        String invoiceSummary = invoices.stream()
                .map(item -> item.invoice().getInvoiceNumber() + " (" + item.outstandingAmount() + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("N/A");
        return "Automatic payment due reminder for " + firstNonBlank(customer.getCompanyName(), "customer")
                + ". Invoices: " + invoiceSummary + ".";
    }

    private String buildMessage(LocalDate fromDate, LocalDate toDate) {
        if (fromDate.equals(toDate)) {
            return "The following invoices are due today. Please arrange payment by the due date.";
        }
        return "The following invoices are due between " + fromDate + " and " + toDate
                + ". Please arrange payment by each due date.";
    }

    private String buildSubject(CustomerProfileEntity customer, LocalDate fromDate, LocalDate toDate) {
        String customerName = firstNonBlank(customer.getCompanyName(), "Customer");
        if (fromDate.equals(toDate)) {
            return "Invoice payment due today - " + customerName;
        }
        return "Upcoming invoice payment reminder - " + customerName;
    }

    private String deriveStatus(InvoiceEntity invoice, BigDecimal paidAmount, BigDecimal outstandingAmount) {
        if (outstandingAmount.compareTo(ZERO) <= 0) {
            return "PAID";
        }
        if (paidAmount.compareTo(ZERO) > 0) {
            return "PARTIALLY_PAID";
        }
        String normalized = normalizeStatus(invoice.getStatus());
        return StringUtils.hasText(normalized) ? normalized : "ISSUED";
    }

    private BigDecimal grandTotal(InvoiceEntity invoice) {
        return normalizeMoney(invoice.getTotalAmount()).add(normalizeMoney(invoice.getVatAmount())).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeStatus(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String resolveCompanyName() {
        return firstNonBlank(emailProperties.getCompanyName(), "G90 Steel");
    }

    private String resolveSupportEmail() {
        return firstNonBlank(emailProperties.getSupportEmail(), "");
    }

    private ZoneId resolveZone() {
        if (!StringUtils.hasText(timeZone)) {
            return DEFAULT_ZONE;
        }
        return ZoneId.of(timeZone.trim());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private record DueInvoiceReminderItem(
            InvoiceEntity invoice,
            BigDecimal grandTotal,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,
            long daysUntilDue,
            String status
    ) {
    }
}
