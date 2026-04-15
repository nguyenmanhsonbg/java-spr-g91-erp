package com.g90.backend.modules.debt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.CustomerProfileNotFoundException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.PaymentNotFoundException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.customer.repository.CustomerManagementRepository;
import com.g90.backend.modules.debt.dto.DebtAgingResponseData;
import com.g90.backend.modules.debt.dto.DebtHistoryResponseData;
import com.g90.backend.modules.debt.dto.DebtListQuery;
import com.g90.backend.modules.debt.dto.DebtListResponseData;
import com.g90.backend.modules.debt.dto.DebtSettlementListResponseData;
import com.g90.backend.modules.debt.dto.DebtStatusResponseData;
import com.g90.backend.modules.debt.dto.OpenInvoiceResponse;
import com.g90.backend.modules.debt.dto.PaymentAllocationRequest;
import com.g90.backend.modules.debt.dto.PaymentCreateRequest;
import com.g90.backend.modules.debt.dto.PaymentResponse;
import com.g90.backend.modules.debt.dto.ReminderCreateRequest;
import com.g90.backend.modules.debt.dto.ReminderHistoryItem;
import com.g90.backend.modules.debt.dto.ReminderListQuery;
import com.g90.backend.modules.debt.dto.ReminderListResponseData;
import com.g90.backend.modules.debt.dto.ReminderSendResponseData;
import com.g90.backend.modules.debt.dto.SettlementConfirmRequest;
import com.g90.backend.modules.debt.dto.SettlementResponse;
import com.g90.backend.modules.debt.entity.DebtInvoiceEntity;
import com.g90.backend.modules.debt.entity.DebtReminderEntity;
import com.g90.backend.modules.debt.entity.DebtSettlementEntity;
import com.g90.backend.modules.debt.entity.PaymentAllocationEntity;
import com.g90.backend.modules.debt.entity.PaymentEntity;
import com.g90.backend.modules.debt.repository.DebtInvoiceRepository;
import com.g90.backend.modules.debt.repository.DebtReminderRepository;
import com.g90.backend.modules.debt.repository.DebtSettlementRepository;
import com.g90.backend.modules.debt.repository.PaymentAllocationRepository;
import com.g90.backend.modules.debt.repository.PaymentRepository;
import com.g90.backend.modules.email.service.EmailService;
import com.g90.backend.modules.product.dto.PaginationResponse;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DebtServiceImpl implements DebtService, PaymentService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal FIFTY_MILLION = new BigDecimal("50000000.00").setScale(2, RoundingMode.HALF_UP);
    private static final Set<String> FINAL_INVOICE_STATUSES = Set.of("PAID", "SETTLED", "CLOSED", "CANCELLED", "VOID");
    private static final Set<String> SKIPPED_INVOICE_STATUSES = Set.of("DRAFT", "CANCELLED", "VOID");
    private static final Set<String> ALLOWED_DEBT_STATUSES = Set.of("NO_DEBT", "OPEN_DEBT", "PARTIALLY_PAID", "OVERDUE", "REMINDER_SENT", "SETTLED");
    private static final Set<String> ALLOWED_PAYMENT_METHODS = Set.of("CASH", "BANK_TRANSFER", "OTHER");
    private static final Set<String> ALLOWED_REMINDER_TYPES = Set.of("GENTLE", "FIRM", "FINAL");
    private static final Set<String> ALLOWED_REMINDER_CHANNELS = Set.of("EMAIL");
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("customerCode", "customerName", "outstandingAmount", "overdueAmount", "lastPaymentDate", "status");
    private static final Set<String> ALLOWED_SORT_DIRECTIONS = Set.of("asc", "desc");
    private static final int MAX_REMINDERS_PER_INVOICE = 3;

    private final CustomerManagementRepository customerManagementRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final DebtInvoiceRepository debtInvoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PaymentExecutionService paymentExecutionService;
    private final DebtReminderRepository debtReminderRepository;
    private final DebtSettlementRepository debtSettlementRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    public DebtServiceImpl(
            CustomerManagementRepository customerManagementRepository,
            CustomerProfileRepository customerProfileRepository,
            DebtInvoiceRepository debtInvoiceRepository,
            PaymentRepository paymentRepository,
            PaymentAllocationRepository paymentAllocationRepository,
            PaymentExecutionService paymentExecutionService,
            DebtReminderRepository debtReminderRepository,
            DebtSettlementRepository debtSettlementRepository,
            CurrentUserProvider currentUserProvider,
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper,
            EmailService emailService
    ) {
        this.customerManagementRepository = customerManagementRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.debtInvoiceRepository = debtInvoiceRepository;
        this.paymentRepository = paymentRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.paymentExecutionService = paymentExecutionService;
        this.debtReminderRepository = debtReminderRepository;
        this.debtSettlementRepository = debtSettlementRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public PaymentResponse recordPayment(PaymentCreateRequest request) {
        AuthenticatedUser currentUser = requireAccountant();
        normalizeAndValidatePaymentRequest(request);

        CustomerProfileEntity customer = findCustomer(request.getCustomerId());
        List<InvoiceBalanceSnapshot> openInvoices = loadOpenInvoiceSnapshots(customer.getId());
        if (openInvoices.isEmpty()) {
            throw RequestValidationException.singleError("customerId", "Customer has no open invoices");
        }

        validatePaymentPolicy(request);
        List<ResolvedAllocation> expectedAllocations = buildExpectedAllocations(openInvoices, request.getAmount());
        validateRequestedAllocations(request.getAllocations(), expectedAllocations, request.getAmount());
        PaymentEntity savedPayment = paymentExecutionService.recordPayment(new PaymentExecutionCommand(
                customer.getId(),
                request.getPaymentDate(),
                request.getAmount(),
                request.getPaymentMethod(),
                request.getReferenceNo(),
                request.getNote(),
                null,
                "CONFIRMED",
                currentUser.userId(),
                expectedAllocations.stream()
                        .map(allocation -> new PaymentExecutionCommand.PaymentExecutionAllocation(
                                allocation.invoice().getId(),
                                allocation.amount()
                        ))
                        .toList()
        ));
        PaymentResponse response = toPaymentResponse(savedPayment, customer);
        logAudit("RECORD_PAYMENT", "PAYMENT", savedPayment.getId(), null, response, currentUser.userId());
        return response;
    }

    @Override
    @Transactional
    public PaymentResponse getPayment(String paymentId) {
        requireAccountant();
        PaymentEntity payment = paymentRepository.findDetailedById(paymentId).orElseThrow(PaymentNotFoundException::new);
        CustomerProfileEntity customer = findCustomer(payment.getCustomerId());
        return toPaymentResponse(payment, customer);
    }

    @Override
    @Transactional
    public List<OpenInvoiceResponse> getOpenInvoices(String customerId) {
        requireAccountant();
        findCustomer(customerId);
        return loadOpenInvoiceSnapshots(customerId).stream()
                .map(this::toOpenInvoiceResponse)
                .toList();
    }

    @Override
    @Transactional
    public DebtListResponseData getDebts(DebtListQuery query) {
        normalizeAndValidateDebtQuery(query);
        List<CustomerDebtData> matched = loadAccessibleCustomersForDebtList().stream()
                .map(customer -> buildCustomerDebtData(customer, false))
                .filter(data -> matchesDebtFilters(data, query))
                .sorted(buildDebtComparator(query))
                .toList();

        int page = query.getPage();
        int pageSize = query.getPageSize();
        int fromIndex = Math.min((page - 1) * pageSize, matched.size());
        int toIndex = Math.min(fromIndex + pageSize, matched.size());
        int totalPages = matched.isEmpty() ? 0 : (int) Math.ceil((double) matched.size() / pageSize);

        return new DebtListResponseData(
                matched.subList(fromIndex, toIndex).stream().map(this::toDebtListItem).toList(),
                PaginationResponse.builder()
                        .page(page)
                        .pageSize(pageSize)
                        .totalItems(matched.size())
                        .totalPages(totalPages)
                        .build(),
                new DebtListResponseData.Filters(
                        query.getKeyword(),
                        query.getCustomerCode(),
                        query.getCustomerName(),
                        query.getInvoiceNumber(),
                        query.getStatus(),
                        query.getOverdueOnly()
                )
        );
    }

    @Override
    @Transactional
    public DebtStatusResponseData getDebtStatus(String customerId) {
        CustomerProfileEntity customer = loadAccessibleCustomer(customerId);
        return toDebtStatusResponse(buildCustomerDebtData(customer, true));
    }

    @Override
    @Transactional
    public DebtAgingResponseData getDebtAging(String customerId) {
        CustomerProfileEntity customer = loadAccessibleCustomer(customerId);
        return toDebtAgingResponse(buildCustomerDebtData(customer, false));
    }

    @Override
    @Transactional
    public DebtHistoryResponseData getDebtHistory(String customerId) {
        CustomerProfileEntity customer = loadAccessibleCustomer(customerId);
        CustomerDebtData data = buildCustomerDebtData(customer, true);
        List<DebtHistoryResponseData.Item> items = new ArrayList<>();
        for (InvoiceBalanceSnapshot invoice : data.invoices()) {
            items.add(new DebtHistoryResponseData.Item(
                    "INVOICE",
                    invoice.invoice().getId(),
                    invoice.invoice().getInvoiceNumber(),
                    invoice.status(),
                    invoice.totalAmount(),
                    "Invoice issued",
                    invoice.invoice().getCreatedAt()
            ));
        }
        for (PaymentResponse payment : data.paymentHistory()) {
            items.add(new DebtHistoryResponseData.Item(
                    "PAYMENT",
                    payment.id(),
                    payment.referenceNo() != null ? payment.referenceNo() : payment.receiptNumber(),
                    payment.paymentMethod(),
                    payment.amount(),
                    "Payment recorded",
                    payment.createdAt()
            ));
        }
        for (ReminderHistoryItem reminder : data.reminderHistory()) {
            items.add(new DebtHistoryResponseData.Item(
                    "REMINDER",
                    reminder.id(),
                    reminder.invoiceNumber(),
                    reminder.status(),
                    null,
                    reminder.reminderType() + " reminder sent via " + reminder.channel(),
                    reminder.sentAt()
            ));
        }
        for (SettlementResponse settlement : data.settlements()) {
            items.add(new DebtHistoryResponseData.Item(
                    "SETTLEMENT",
                    settlement.id(),
                    settlement.id(),
                    settlement.status(),
                    ZERO,
                    "Debt settlement confirmed",
                    settlement.createdAt()
            ));
        }
        items.sort(Comparator.comparing(DebtHistoryResponseData.Item::eventAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return new DebtHistoryResponseData(items);
    }

    @Override
    @Transactional
    public ReminderSendResponseData sendReminder(ReminderCreateRequest request) {
        AuthenticatedUser currentUser = requireAccountant();
        normalizeAndValidateReminderRequest(request);

        CustomerProfileEntity customer = findCustomer(request.getCustomerId());
        if (!StringUtils.hasText(customer.getEmail())) {
            throw RequestValidationException.singleError("customerId", "Customer does not have a valid email address");
        }

        List<InvoiceBalanceSnapshot> openInvoices = loadOpenInvoiceSnapshots(customer.getId());
        Map<String, InvoiceBalanceSnapshot> invoiceMap = new LinkedHashMap<>();
        for (InvoiceBalanceSnapshot invoice : openInvoices) {
            invoiceMap.put(invoice.invoice().getId(), invoice);
        }

        LinkedHashSet<String> uniqueInvoiceIds = new LinkedHashSet<>(request.getInvoiceIds());
        List<InvoiceBalanceSnapshot> selectedInvoices = new ArrayList<>();
        for (String invoiceId : uniqueInvoiceIds) {
            InvoiceBalanceSnapshot invoice = invoiceMap.get(invoiceId);
            if (invoice == null) {
                throw RequestValidationException.singleError("invoiceIds", "Invoice " + invoiceId + " is not an overdue open invoice for the selected customer");
            }
            if (invoice.overdueDays() <= 0) {
                throw RequestValidationException.singleError("invoiceIds", "Reminder can only be sent for overdue invoices");
            }
            if (debtReminderRepository.countByInvoice_Id(invoiceId) >= MAX_REMINDERS_PER_INVOICE) {
                throw RequestValidationException.singleError("invoiceIds", "Reminder limit reached for invoice " + invoice.invoice().getInvoiceNumber());
            }
            selectedInvoices.add(invoice);
        }

        String content = buildReminderContent(customer, selectedInvoices, request);
        LocalDateTime sentAt = LocalDateTime.now(APP_ZONE);
        emailService.sendHtmlEmail(
                customer.getEmail(),
                buildReminderSubject(request.getReminderType(), customer),
                "debt-reminder",
                Map.of(
                        "recipientName", resolveRecipientName(customer),
                        "customerName", customer.getCompanyName(),
                        "reminderType", request.getReminderType(),
                        "message", content,
                        "sentAt", sentAt,
                        "invoices", selectedInvoices.stream().map(this::toOpenInvoiceResponse).toList()
                )
        ).join();

        List<DebtReminderEntity> reminders = new ArrayList<>();
        for (InvoiceBalanceSnapshot invoice : selectedInvoices) {
            DebtReminderEntity reminder = new DebtReminderEntity();
            reminder.setCustomerId(customer.getId());
            reminder.setInvoice(invoice.invoice());
            reminder.setReminderType(request.getReminderType());
            reminder.setChannel(request.getChannel());
            reminder.setContent(content);
            reminder.setNote(request.getNote());
            reminder.setSentBy(currentUser.userId());
            reminder.setSentAt(sentAt);
            reminder.setStatus("SENT");
            reminders.add(reminder);
        }

        List<DebtReminderEntity> saved = debtReminderRepository.saveAll(reminders);
        List<ReminderHistoryItem> items = saved.stream()
                .map(reminder -> toReminderHistoryItem(reminder, customer))
                .toList();
        ReminderSendResponseData response = new ReminderSendResponseData(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getCompanyName(),
                request.getReminderType(),
                request.getChannel(),
                items.size(),
                sentAt,
                items
        );
        logAudit("SEND_DEBT_REMINDER", "DEBT_REMINDER", customer.getId(), null, response, currentUser.userId());
        return response;
    }

    @Override
    @Transactional
    public ReminderListResponseData getReminders(ReminderListQuery query) {
        requireAccountant();
        normalizeAndValidateReminderQuery(query);
        List<ReminderHistoryItem> matched = debtReminderRepository.findAllByOrderBySentAtDesc().stream()
                .filter(reminder -> matchesReminderQuery(reminder, query))
                .map(this::toReminderHistoryItem)
                .toList();

        int page = query.getPage();
        int pageSize = query.getPageSize();
        int fromIndex = Math.min((page - 1) * pageSize, matched.size());
        int toIndex = Math.min(fromIndex + pageSize, matched.size());
        int totalPages = matched.isEmpty() ? 0 : (int) Math.ceil((double) matched.size() / pageSize);
        return new ReminderListResponseData(
                matched.subList(fromIndex, toIndex),
                PaginationResponse.builder()
                        .page(page)
                        .pageSize(pageSize)
                        .totalItems(matched.size())
                        .totalPages(totalPages)
                        .build(),
                new ReminderListResponseData.Filters(
                        query.getCustomerId(),
                        query.getInvoiceNumber(),
                        query.getStatus(),
                        query.getReminderType(),
                        query.getChannel()
                )
        );
    }

    @Override
    @Transactional
    public SettlementResponse confirmSettlement(String customerId, SettlementConfirmRequest request) {
        AuthenticatedUser currentUser = requireAccountant();
        CustomerProfileEntity customer = findCustomer(customerId);
        CustomerDebtData data = buildCustomerDebtData(customer, false);
        if (data.outstandingAmount().compareTo(ZERO) > 0) {
            throw RequestValidationException.singleError("customerId", "Outstanding balance must be zero before settlement can be confirmed");
        }
        boolean hasUnsettledInvoice = data.invoices().stream()
                .anyMatch(invoice -> !FINAL_INVOICE_STATUSES.contains(invoice.status()));
        if (hasUnsettledInvoice) {
            throw RequestValidationException.singleError("customerId", "All related invoices must be fully paid before settlement confirmation");
        }

        DebtSettlementEntity settlement = new DebtSettlementEntity();
        settlement.setCustomerId(customer.getId());
        settlement.setConfirmedBy(currentUser.userId());
        settlement.setNote(normalizeNullable(request.getNote()));
        settlement.setCertificateUrl(null);
        DebtSettlementEntity saved = debtSettlementRepository.save(settlement);
        SettlementResponse response = toSettlementResponse(saved, customer);
        logAudit("CONFIRM_DEBT_SETTLEMENT", "DEBT_SETTLEMENT", saved.getId(), null, response, currentUser.userId());
        return response;
    }

    @Override
    @Transactional
    public DebtSettlementListResponseData getSettlements(String customerId) {
        CustomerProfileEntity customer = loadAccessibleCustomer(customerId);
        return new DebtSettlementListResponseData(
                debtSettlementRepository.findByCustomerIdOrderBySettlementDateDescCreatedAtDesc(customer.getId()).stream()
                        .map(settlement -> toSettlementResponse(settlement, customer))
                        .toList()
        );
    }

    @Override
    @Transactional
    public byte[] exportDebts(DebtListQuery query) {
        requireAccountant();
        normalizeAndValidateDebtQuery(query);
        List<DebtListResponseData.Item> items = loadAccessibleCustomersForDebtList().stream()
                .map(customer -> buildCustomerDebtData(customer, false))
                .filter(data -> matchesDebtFilters(data, query))
                .sorted(buildDebtComparator(query))
                .map(this::toDebtListItem)
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("Customer Code,Customer Name,Credit Limit,Outstanding Amount,Overdue Amount,Current Bucket,30 Day Bucket,60 Day Bucket,90+ Day Bucket,Last Payment Date,Status\n");
        for (DebtListResponseData.Item item : items) {
            csv.append(csv(item.customerCode())).append(',')
                    .append(csv(item.customerName())).append(',')
                    .append(csv(item.creditLimit())).append(',')
                    .append(csv(item.outstandingAmount())).append(',')
                    .append(csv(item.overdueAmount())).append(',')
                    .append(csv(item.currentBucket())).append(',')
                    .append(csv(item.bucket30())).append(',')
                    .append(csv(item.bucket60())).append(',')
                    .append(csv(item.bucket90Plus())).append(',')
                    .append(csv(item.lastPaymentDate())).append(',')
                    .append(csv(item.status())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private CustomerDebtData buildCustomerDebtData(CustomerProfileEntity customer, boolean includeDetailLists) {
        List<InvoiceBalanceSnapshot> invoices = loadInvoiceBalances(customer.getId());
        BigDecimal totalInvoiceAmount = ZERO;
        BigDecimal totalAllocatedAmount = ZERO;
        BigDecimal outstandingAmount = ZERO;
        BigDecimal overdueAmount = ZERO;
        BigDecimal currentBucket = ZERO;
        BigDecimal bucket30 = ZERO;
        BigDecimal bucket60 = ZERO;
        BigDecimal bucket90Plus = ZERO;
        int partialInvoiceCount = 0;

        for (InvoiceBalanceSnapshot invoice : invoices) {
            if (SKIPPED_INVOICE_STATUSES.contains(invoice.baseStatus())) {
                continue;
            }
            totalInvoiceAmount = totalInvoiceAmount.add(invoice.totalAmount());
            totalAllocatedAmount = totalAllocatedAmount.add(invoice.paidAmount());
            outstandingAmount = outstandingAmount.add(invoice.remainingAmount());
            if (invoice.remainingAmount().compareTo(ZERO) > 0) {
                if (invoice.paidAmount().compareTo(ZERO) > 0) {
                    partialInvoiceCount++;
                }
                if (invoice.overdueDays() <= 0) {
                    currentBucket = currentBucket.add(invoice.remainingAmount());
                } else if (invoice.overdueDays() <= 30) {
                    bucket30 = bucket30.add(invoice.remainingAmount());
                    overdueAmount = overdueAmount.add(invoice.remainingAmount());
                } else if (invoice.overdueDays() <= 60) {
                    bucket60 = bucket60.add(invoice.remainingAmount());
                    overdueAmount = overdueAmount.add(invoice.remainingAmount());
                } else {
                    bucket90Plus = bucket90Plus.add(invoice.remainingAmount());
                    overdueAmount = overdueAmount.add(invoice.remainingAmount());
                }
            }
        }

        List<PaymentEntity> payments = paymentRepository.findByCustomerIdOrderByPaymentDateDescCreatedAtDesc(customer.getId());
        BigDecimal totalPaymentsReceived = payments.stream()
                .map(payment -> normalizeMoney(payment.getAmount()))
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        LocalDate lastPaymentDate = payments.isEmpty() ? null : payments.get(0).getPaymentDate();

        List<DebtReminderEntity> reminderEntities = debtReminderRepository.findByCustomerIdOrderBySentAtDesc(customer.getId());
        List<ReminderHistoryItem> reminderHistory = includeDetailLists
                ? reminderEntities.stream().map(reminder -> toReminderHistoryItem(reminder, customer)).toList()
                : List.of();
        List<DebtSettlementEntity> settlementEntities = debtSettlementRepository.findByCustomerIdOrderBySettlementDateDescCreatedAtDesc(customer.getId());
        List<SettlementResponse> settlements = includeDetailLists
                ? settlementEntities.stream().map(settlement -> toSettlementResponse(settlement, customer)).toList()
                : List.of();

        List<OpenInvoiceResponse> openInvoices = invoices.stream()
                .filter(invoice -> invoice.remainingAmount().compareTo(ZERO) > 0 && !SKIPPED_INVOICE_STATUSES.contains(invoice.baseStatus()))
                .map(this::toOpenInvoiceResponse)
                .toList();
        List<PaymentResponse> paymentHistory = includeDetailLists
                ? payments.stream().map(payment -> toPaymentResponse(payment, customer)).toList()
                : List.of();

        return new CustomerDebtData(
                customer,
                totalInvoiceAmount,
                totalPaymentsReceived,
                totalAllocatedAmount,
                outstandingAmount,
                overdueAmount,
                currentBucket,
                bucket30,
                bucket60,
                bucket90Plus,
                lastPaymentDate,
                deriveDebtStatus(outstandingAmount, overdueAmount, partialInvoiceCount, !reminderEntities.isEmpty(), !settlementEntities.isEmpty()),
                invoices,
                openInvoices,
                paymentHistory,
                reminderHistory,
                settlements
        );
    }

    private List<CustomerProfileEntity> loadAccessibleCustomersForDebtList() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        RoleName role = roleOf(currentUser);
        if (role == RoleName.CUSTOMER) {
            return List.of(loadCurrentCustomer());
        }
        if (role != RoleName.ACCOUNTANT) {
            throw new ForbiddenOperationException("Only accountant and customer users can access debt information");
        }
        return customerManagementRepository.findAll(Sort.by(Sort.Direction.ASC, "customerCode"));
    }

    private CustomerProfileEntity loadAccessibleCustomer(String customerId) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        RoleName role = roleOf(currentUser);
        if (role == RoleName.CUSTOMER) {
            CustomerProfileEntity currentCustomer = loadCurrentCustomer();
            if (!currentCustomer.getId().equals(customerId)) {
                throw new CustomerProfileNotFoundException();
            }
            return currentCustomer;
        }
        if (role != RoleName.ACCOUNTANT) {
            throw new ForbiddenOperationException("Only accountant and customer users can access debt information");
        }
        return findCustomer(customerId);
    }

    private CustomerProfileEntity loadCurrentCustomer() {
        return customerProfileRepository.findByUser_Id(currentUserProvider.getCurrentUser().userId())
                .orElseThrow(CustomerProfileNotFoundException::new);
    }

    private CustomerProfileEntity findCustomer(String customerId) {
        return customerManagementRepository.findDetailedById(customerId)
                .orElseThrow(CustomerProfileNotFoundException::new);
    }

    private AuthenticatedUser requireAccountant() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (roleOf(currentUser) != RoleName.ACCOUNTANT) {
            throw new ForbiddenOperationException("Only accountant users can perform this action");
        }
        return currentUser;
    }

    private RoleName roleOf(AuthenticatedUser currentUser) {
        return RoleName.from(currentUser.role());
    }

    private List<InvoiceBalanceSnapshot> loadOpenInvoiceSnapshots(String customerId) {
        return loadInvoiceBalances(customerId).stream()
                .filter(invoice -> invoice.remainingAmount().compareTo(ZERO) > 0 && !SKIPPED_INVOICE_STATUSES.contains(invoice.baseStatus()))
                .toList();
    }

    private List<InvoiceBalanceSnapshot> loadInvoiceBalances(String customerId) {
        List<DebtInvoiceEntity> invoices = debtInvoiceRepository.findByCustomerIdOrderByDueDateAscCreatedAtAsc(customerId);
        if (invoices.isEmpty()) {
            return List.of();
        }
        Map<String, BigDecimal> allocatedMap = new LinkedHashMap<>();
        List<String> invoiceIds = invoices.stream().map(DebtInvoiceEntity::getId).toList();
        for (PaymentAllocationRepository.InvoiceAllocationTotalView view : paymentAllocationRepository.summarizeByInvoiceIds(invoiceIds)) {
            allocatedMap.put(view.getInvoiceId(), normalizeMoney(view.getAllocatedAmount()));
        }

        List<InvoiceBalanceSnapshot> snapshots = new ArrayList<>();
        for (DebtInvoiceEntity invoice : invoices) {
            BigDecimal totalAmount = normalizeMoney(invoice.getTotalAmount()).add(normalizeMoney(invoice.getVatAmount())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal paidAmount = allocatedMap.getOrDefault(invoice.getId(), ZERO).min(totalAmount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal remainingAmount = totalAmount.subtract(paidAmount).max(ZERO).setScale(2, RoundingMode.HALF_UP);
            long overdueDays = 0L;
            if (invoice.getDueDate() != null && remainingAmount.compareTo(ZERO) > 0 && invoice.getDueDate().isBefore(LocalDate.now(APP_ZONE))) {
                overdueDays = ChronoUnit.DAYS.between(invoice.getDueDate(), LocalDate.now(APP_ZONE));
            }
            String baseStatus = normalizeUpper(invoice.getStatus());
            snapshots.add(new InvoiceBalanceSnapshot(
                    invoice,
                    totalAmount,
                    paidAmount,
                    remainingAmount,
                    deriveInvoiceStatus(baseStatus, paidAmount, remainingAmount, overdueDays),
                    baseStatus,
                    overdueDays
            ));
        }
        return snapshots;
    }

    private Map<String, InvoiceBalanceSnapshot> loadInvoiceBalanceMap(String customerId) {
        Map<String, InvoiceBalanceSnapshot> result = new LinkedHashMap<>();
        for (InvoiceBalanceSnapshot snapshot : loadInvoiceBalances(customerId)) {
            result.put(snapshot.invoice().getId(), snapshot);
        }
        return result;
    }

    private void updateTouchedInvoiceStatuses(List<ResolvedAllocation> allocations) {
        if (allocations.isEmpty()) {
            return;
        }
        String customerId = allocations.get(0).invoice().getCustomerId();
        Map<String, InvoiceBalanceSnapshot> latestSnapshots = loadInvoiceBalanceMap(customerId);
        List<DebtInvoiceEntity> touchedInvoices = new ArrayList<>();
        for (ResolvedAllocation allocation : allocations) {
            InvoiceBalanceSnapshot latest = latestSnapshots.get(allocation.invoice().getId());
            if (latest == null) {
                continue;
            }
            DebtInvoiceEntity invoice = allocation.invoice();
            invoice.setStatus(latest.remainingAmount().compareTo(ZERO) <= 0 ? "PAID" : "PARTIALLY_PAID");
            touchedInvoices.add(invoice);
        }
        if (!touchedInvoices.isEmpty()) {
            debtInvoiceRepository.saveAll(touchedInvoices);
        }
    }

    private List<ResolvedAllocation> buildExpectedAllocations(List<InvoiceBalanceSnapshot> openInvoices, BigDecimal paymentAmount) {
        BigDecimal remaining = paymentAmount;
        List<ResolvedAllocation> allocations = new ArrayList<>();
        for (InvoiceBalanceSnapshot invoice : openInvoices) {
            if (remaining.compareTo(ZERO) <= 0) {
                break;
            }
            BigDecimal allocated = remaining.min(invoice.remainingAmount()).setScale(2, RoundingMode.HALF_UP);
            if (allocated.compareTo(ZERO) > 0) {
                allocations.add(new ResolvedAllocation(invoice.invoice(), allocated));
                remaining = remaining.subtract(allocated).setScale(2, RoundingMode.HALF_UP);
            }
        }
        if (remaining.compareTo(ZERO) > 0) {
            throw RequestValidationException.singleError("amount", "Payment amount exceeds outstanding balance");
        }
        return allocations;
    }

    private void validateRequestedAllocations(
            List<PaymentAllocationRequest> requestedAllocations,
            List<ResolvedAllocation> expectedAllocations,
            BigDecimal paymentAmount
    ) {
        if (requestedAllocations == null || requestedAllocations.isEmpty()) {
            return;
        }
        Map<String, BigDecimal> requested = new LinkedHashMap<>();
        BigDecimal total = ZERO;
        for (PaymentAllocationRequest allocation : requestedAllocations) {
            String invoiceId = normalizeRequired(allocation.getInvoiceId(), "allocations.invoiceId", "invoiceId is required");
            BigDecimal amount = normalizePositiveMoney(allocation.getAllocatedAmount(), "allocations.allocatedAmount", "allocatedAmount must be greater than 0");
            if (requested.put(invoiceId, amount) != null) {
                throw RequestValidationException.singleError("allocations", "Duplicate invoice allocation is not allowed");
            }
            total = total.add(amount).setScale(2, RoundingMode.HALF_UP);
        }
        if (total.compareTo(paymentAmount) != 0) {
            throw RequestValidationException.singleError("allocations", "Allocation total must equal payment amount");
        }

        Map<String, BigDecimal> expected = new LinkedHashMap<>();
        for (ResolvedAllocation allocation : expectedAllocations) {
            expected.put(allocation.invoice().getId(), allocation.amount());
        }
        if (!expected.equals(requested)) {
            throw RequestValidationException.singleError("allocations", "Payment allocation must follow FIFO across open invoices");
        }
    }

    private String deriveDebtStatus(
            BigDecimal outstandingAmount,
            BigDecimal overdueAmount,
            int partialInvoiceCount,
            boolean hasReminder,
            boolean hasSettlement
    ) {
        if (outstandingAmount.compareTo(ZERO) <= 0) {
            return hasSettlement ? "SETTLED" : "NO_DEBT";
        }
        if (overdueAmount.compareTo(ZERO) > 0) {
            return hasReminder ? "REMINDER_SENT" : "OVERDUE";
        }
        if (partialInvoiceCount > 0) {
            return "PARTIALLY_PAID";
        }
        return "OPEN_DEBT";
    }

    private String deriveInvoiceStatus(String baseStatus, BigDecimal paidAmount, BigDecimal remainingAmount, long overdueDays) {
        if (SKIPPED_INVOICE_STATUSES.contains(baseStatus)) {
            return baseStatus;
        }
        if (remainingAmount.compareTo(ZERO) <= 0) {
            return "PAID";
        }
        if (paidAmount.compareTo(ZERO) > 0) {
            return "PARTIALLY_PAID";
        }
        if (overdueDays > 0) {
            return "OVERDUE";
        }
        return StringUtils.hasText(baseStatus) ? baseStatus : "OPEN";
    }

    private DebtStatusResponseData toDebtStatusResponse(CustomerDebtData data) {
        return new DebtStatusResponseData(
                new DebtStatusResponseData.Summary(
                        data.customer().getId(),
                        data.customer().getCustomerCode(),
                        data.customer().getCompanyName(),
                        normalizeMoney(data.customer().getCreditLimit()),
                        data.customer().getPaymentTerms(),
                        data.totalInvoiceAmount(),
                        data.totalPaymentsReceived(),
                        data.totalAllocatedPayments(),
                        data.outstandingAmount(),
                        data.overdueAmount(),
                        data.lastPaymentDate(),
                        data.status()
                ),
                toDebtAgingResponse(data),
                data.openInvoices(),
                data.paymentHistory(),
                data.reminderHistory(),
                data.settlements()
        );
    }

    private DebtAgingResponseData toDebtAgingResponse(CustomerDebtData data) {
        return new DebtAgingResponseData(
                data.outstandingAmount(),
                data.overdueAmount(),
                data.currentBucket(),
                data.bucket30(),
                data.bucket60(),
                data.bucket90Plus()
        );
    }

    private DebtListResponseData.Item toDebtListItem(CustomerDebtData data) {
        return new DebtListResponseData.Item(
                data.customer().getId(),
                data.customer().getCustomerCode(),
                data.customer().getCompanyName(),
                normalizeMoney(data.customer().getCreditLimit()),
                data.outstandingAmount(),
                data.overdueAmount(),
                data.currentBucket(),
                data.bucket30(),
                data.bucket60(),
                data.bucket90Plus(),
                data.lastPaymentDate(),
                data.status()
        );
    }

    private PaymentResponse toPaymentResponse(PaymentEntity payment, CustomerProfileEntity customer) {
        Map<String, InvoiceBalanceSnapshot> latestInvoices = loadInvoiceBalanceMap(customer.getId());
        List<PaymentResponse.AllocationItem> allocations = payment.getAllocations() == null ? List.of() : payment.getAllocations().stream()
                .map(allocation -> {
                    InvoiceBalanceSnapshot latest = latestInvoices.get(allocation.getInvoice().getId());
                    return new PaymentResponse.AllocationItem(
                            allocation.getInvoice().getId(),
                            allocation.getInvoice().getInvoiceNumber(),
                            normalizeMoney(allocation.getAmount()),
                            latest == null ? ZERO : latest.remainingAmount(),
                            latest == null ? normalizeUpper(allocation.getInvoice().getStatus()) : latest.status()
                    );
                })
                .toList();
        return new PaymentResponse(
                payment.getId(),
                buildReceiptNumber(payment.getId()),
                customer.getId(),
                customer.getCustomerCode(),
                customer.getCompanyName(),
                payment.getPaymentDate(),
                normalizeMoney(payment.getAmount()),
                payment.getPaymentMethod(),
                payment.getReferenceNo(),
                payment.getNote(),
                normalizeUpper(payment.getStatus()),
                payment.getProofDocumentUrl(),
                payment.getCreatedBy(),
                payment.getUpdatedBy(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                allocations
        );
    }

    private OpenInvoiceResponse toOpenInvoiceResponse(InvoiceBalanceSnapshot invoice) {
        return new OpenInvoiceResponse(
                invoice.invoice().getId(),
                invoice.invoice().getInvoiceNumber(),
                invoice.invoice().getCreatedAt(),
                invoice.invoice().getDueDate(),
                invoice.totalAmount(),
                invoice.paidAmount(),
                invoice.remainingAmount(),
                invoice.status(),
                invoice.overdueDays()
        );
    }

    private ReminderHistoryItem toReminderHistoryItem(DebtReminderEntity reminder) {
        return toReminderHistoryItem(reminder, findCustomer(reminder.getCustomerId()));
    }

    private ReminderHistoryItem toReminderHistoryItem(DebtReminderEntity reminder, CustomerProfileEntity customer) {
        return new ReminderHistoryItem(
                reminder.getId(),
                customer.getId(),
                customer.getCustomerCode(),
                customer.getCompanyName(),
                reminder.getInvoice().getId(),
                reminder.getInvoice().getInvoiceNumber(),
                reminder.getReminderType(),
                reminder.getChannel(),
                reminder.getContent(),
                reminder.getStatus(),
                reminder.getSentBy(),
                reminder.getSentAt()
        );
    }

    private SettlementResponse toSettlementResponse(DebtSettlementEntity settlement, CustomerProfileEntity customer) {
        return new SettlementResponse(
                settlement.getId(),
                customer.getId(),
                customer.getCustomerCode(),
                customer.getCompanyName(),
                settlement.getSettlementDate(),
                settlement.getConfirmedBy(),
                settlement.getNote(),
                settlement.getCertificateUrl(),
                "SETTLED",
                settlement.getCreatedAt()
        );
    }

    private boolean matchesDebtFilters(CustomerDebtData data, DebtListQuery query) {
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().toLowerCase(Locale.ROOT);
            boolean matched = containsIgnoreCase(data.customer().getCustomerCode(), keyword)
                    || containsIgnoreCase(data.customer().getCompanyName(), keyword);
            if (!matched) {
                return false;
            }
        }
        if (StringUtils.hasText(query.getCustomerCode()) && !containsIgnoreCase(data.customer().getCustomerCode(), query.getCustomerCode().toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (StringUtils.hasText(query.getCustomerName()) && !containsIgnoreCase(data.customer().getCompanyName(), query.getCustomerName().toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (StringUtils.hasText(query.getInvoiceNumber())) {
            boolean matched = data.invoices().stream()
                    .map(snapshot -> snapshot.invoice().getInvoiceNumber())
                    .anyMatch(invoiceNumber -> containsIgnoreCase(invoiceNumber, query.getInvoiceNumber().toLowerCase(Locale.ROOT)));
            if (!matched) {
                return false;
            }
        }
        if (Boolean.TRUE.equals(query.getOverdueOnly()) && data.overdueAmount().compareTo(ZERO) <= 0) {
            return false;
        }
        return !StringUtils.hasText(query.getStatus()) || query.getStatus().equalsIgnoreCase(data.status());
    }

    private boolean matchesReminderQuery(DebtReminderEntity reminder, ReminderListQuery query) {
        if (StringUtils.hasText(query.getCustomerId()) && !query.getCustomerId().equalsIgnoreCase(reminder.getCustomerId())) {
            return false;
        }
        if (StringUtils.hasText(query.getInvoiceNumber()) && !containsIgnoreCase(reminder.getInvoice().getInvoiceNumber(), query.getInvoiceNumber().toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (StringUtils.hasText(query.getStatus()) && !query.getStatus().equalsIgnoreCase(reminder.getStatus())) {
            return false;
        }
        if (StringUtils.hasText(query.getReminderType()) && !query.getReminderType().equalsIgnoreCase(reminder.getReminderType())) {
            return false;
        }
        return !StringUtils.hasText(query.getChannel()) || query.getChannel().equalsIgnoreCase(reminder.getChannel());
    }

    private Comparator<CustomerDebtData> buildDebtComparator(DebtListQuery query) {
        Comparator<CustomerDebtData> comparator = switch (query.getSortBy()) {
            case "customerCode" -> Comparator.comparing(data -> normalizeNullable(data.customer().getCustomerCode()), Comparator.nullsLast(String::compareToIgnoreCase));
            case "customerName" -> Comparator.comparing(data -> normalizeNullable(data.customer().getCompanyName()), Comparator.nullsLast(String::compareToIgnoreCase));
            case "overdueAmount" -> Comparator.comparing(CustomerDebtData::overdueAmount);
            case "lastPaymentDate" -> Comparator.comparing(CustomerDebtData::lastPaymentDate, Comparator.nullsLast(LocalDate::compareTo));
            case "status" -> Comparator.comparing(CustomerDebtData::status, String::compareToIgnoreCase);
            default -> Comparator.comparing(CustomerDebtData::outstandingAmount);
        };
        return "desc".equalsIgnoreCase(query.getSortDir()) ? comparator.reversed() : comparator;
    }

    private void normalizeAndValidateDebtQuery(DebtListQuery query) {
        query.setKeyword(normalizeNullable(query.getKeyword()));
        query.setCustomerCode(normalizeNullable(query.getCustomerCode()));
        query.setCustomerName(normalizeNullable(query.getCustomerName()));
        query.setInvoiceNumber(normalizeNullable(query.getInvoiceNumber()));
        query.setStatus(normalizeNullableUpper(query.getStatus()));
        query.setPage(query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage());
        query.setPageSize(query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize());
        query.setSortBy(StringUtils.hasText(query.getSortBy()) ? query.getSortBy().trim() : "outstandingAmount");
        query.setSortDir(StringUtils.hasText(query.getSortDir()) ? query.getSortDir().trim().toLowerCase(Locale.ROOT) : "desc");
        if (StringUtils.hasText(query.getStatus()) && !ALLOWED_DEBT_STATUSES.contains(query.getStatus())) {
            throw RequestValidationException.singleError("status", "status must be one of NO_DEBT, OPEN_DEBT, PARTIALLY_PAID, OVERDUE, REMINDER_SENT, SETTLED");
        }
        if (!ALLOWED_SORT_FIELDS.contains(query.getSortBy())) {
            throw RequestValidationException.singleError("sortBy", "sortBy must be one of customerCode, customerName, outstandingAmount, overdueAmount, lastPaymentDate, status");
        }
        if (!ALLOWED_SORT_DIRECTIONS.contains(query.getSortDir())) {
            throw RequestValidationException.singleError("sortDir", "sortDir must be asc or desc");
        }
    }

    private void normalizeAndValidateReminderQuery(ReminderListQuery query) {
        query.setCustomerId(normalizeNullable(query.getCustomerId()));
        query.setInvoiceNumber(normalizeNullable(query.getInvoiceNumber()));
        query.setStatus(normalizeNullableUpper(query.getStatus()));
        query.setReminderType(normalizeNullableUpper(query.getReminderType()));
        query.setChannel(normalizeNullableUpper(query.getChannel()));
        query.setPage(query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage());
        query.setPageSize(query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize());
    }

    private void normalizeAndValidatePaymentRequest(PaymentCreateRequest request) {
        request.setCustomerId(normalizeRequired(request.getCustomerId(), "customerId", "customerId is required"));
        if (request.getPaymentDate() == null) {
            throw RequestValidationException.singleError("paymentDate", "paymentDate is required");
        }
        request.setAmount(normalizePositiveMoney(request.getAmount(), "amount", "amount must be greater than 0"));
        request.setPaymentMethod(resolvePaymentMethod(request.getPaymentMethod()));
        request.setReferenceNo(normalizeNullable(request.getReferenceNo()));
        request.setNote(normalizeNullable(request.getNote()));
        if ("BANK_TRANSFER".equals(request.getPaymentMethod()) && !StringUtils.hasText(request.getReferenceNo())) {
            throw RequestValidationException.singleError("referenceNo", "referenceNo is required for bank transfer payments");
        }
    }

    private void normalizeAndValidateReminderRequest(ReminderCreateRequest request) {
        request.setCustomerId(normalizeRequired(request.getCustomerId(), "customerId", "customerId is required"));
        request.setReminderType(resolveReminderType(request.getReminderType()));
        request.setChannel(resolveReminderChannel(request.getChannel()));
        request.setMessage(normalizeNullable(request.getMessage()));
        request.setNote(normalizeNullable(request.getNote()));
    }

    private void validatePaymentPolicy(PaymentCreateRequest request) {
        if ("CASH".equals(request.getPaymentMethod()) && request.getAmount().compareTo(FIFTY_MILLION) > 0) {
            throw RequestValidationException.singleError("amount", "Cash payments over 50,000,000 VND require dual approval and are not supported in this API");
        }
    }

    private String buildReminderContent(CustomerProfileEntity customer, List<InvoiceBalanceSnapshot> invoices, ReminderCreateRequest request) {
        if (StringUtils.hasText(request.getMessage())) {
            return request.getMessage();
        }
        String prefix = switch (request.getReminderType()) {
            case "FINAL" -> "This is the final reminder for your overdue invoices.";
            case "FIRM" -> "Your account has overdue invoices that require immediate attention.";
            default -> "This is a friendly reminder about your overdue invoices.";
        };
        String invoiceSummary = invoices.stream()
                .map(invoice -> invoice.invoice().getInvoiceNumber() + " (" + invoice.remainingAmount() + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("N/A");
        return prefix + " Customer: " + customer.getCompanyName() + ". Invoices: " + invoiceSummary + ".";
    }

    private String buildReminderSubject(String reminderType, CustomerProfileEntity customer) {
        return switch (reminderType) {
            case "FINAL" -> "Final payment reminder - " + customer.getCompanyName();
            case "FIRM" -> "Urgent payment reminder - " + customer.getCompanyName();
            default -> "Payment reminder - " + customer.getCompanyName();
        };
    }

    private String resolveRecipientName(CustomerProfileEntity customer) {
        return StringUtils.hasText(customer.getContactPerson()) ? customer.getContactPerson().trim() : customer.getCompanyName();
    }

    private String buildReceiptNumber(String paymentId) {
        String suffix = paymentId == null ? "UNKNOWN" : paymentId.replace("-", "").toUpperCase(Locale.ROOT);
        return "RCPT-" + suffix.substring(0, Math.min(8, suffix.length()));
    }

    private void logAudit(String action, String entityType, String entityId, Object oldValue, Object newValue, String userId) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setOldValue(toJson(oldValue));
        auditLog.setNewValue(toJson(newValue));
        auditLogRepository.save(auditLog);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize audit payload", exception);
        }
    }

    private BigDecimal normalizePositiveMoney(BigDecimal value, String field, String message) {
        BigDecimal normalized = normalizeMoney(value);
        if (normalized.compareTo(ZERO) <= 0) {
            throw RequestValidationException.singleError(field, message);
        }
        return normalized;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolvePaymentMethod(String value) {
        String normalized = normalizeRequired(value, "paymentMethod", "paymentMethod is required").toUpperCase(Locale.ROOT);
        if ("BANK".equals(normalized)) {
            normalized = "BANK_TRANSFER";
        }
        if (!ALLOWED_PAYMENT_METHODS.contains(normalized)) {
            throw RequestValidationException.singleError("paymentMethod", "paymentMethod must be one of CASH, BANK_TRANSFER, OTHER");
        }
        return normalized;
    }

    private String resolveReminderType(String value) {
        String normalized = normalizeRequired(value, "reminderType", "reminderType is required").toUpperCase(Locale.ROOT);
        if (!ALLOWED_REMINDER_TYPES.contains(normalized)) {
            throw RequestValidationException.singleError("reminderType", "reminderType must be one of GENTLE, FIRM, FINAL");
        }
        return normalized;
    }

    private String resolveReminderChannel(String value) {
        String normalized = normalizeRequired(value, "channel", "channel is required").toUpperCase(Locale.ROOT);
        if (!ALLOWED_REMINDER_CHANNELS.contains(normalized)) {
            throw RequestValidationException.singleError("channel", "Only EMAIL reminder channel is currently supported");
        }
        return normalized;
    }

    private String normalizeRequired(String value, String field, String message) {
        if (!StringUtils.hasText(value)) {
            throw RequestValidationException.singleError(field, message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String normalizeNullableUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private boolean containsIgnoreCase(String value, String lowerCaseQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerCaseQuery);
    }

    private String csv(Object value) {
        String raw = value == null ? "" : value.toString();
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }

    private record InvoiceBalanceSnapshot(
            DebtInvoiceEntity invoice,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal remainingAmount,
            String status,
            String baseStatus,
            long overdueDays
    ) {
    }

    private record ResolvedAllocation(
            DebtInvoiceEntity invoice,
            BigDecimal amount
    ) {
    }

    private record CustomerDebtData(
            CustomerProfileEntity customer,
            BigDecimal totalInvoiceAmount,
            BigDecimal totalPaymentsReceived,
            BigDecimal totalAllocatedPayments,
            BigDecimal outstandingAmount,
            BigDecimal overdueAmount,
            BigDecimal currentBucket,
            BigDecimal bucket30,
            BigDecimal bucket60,
            BigDecimal bucket90Plus,
            LocalDate lastPaymentDate,
            String status,
            List<InvoiceBalanceSnapshot> invoices,
            List<OpenInvoiceResponse> openInvoices,
            List<PaymentResponse> paymentHistory,
            List<ReminderHistoryItem> reminderHistory,
            List<SettlementResponse> settlements
    ) {
    }
}
