package com.g90.backend.modules.dashboard.service;

import com.g90.backend.exception.CustomerProfileNotFoundException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.contract.entity.ContractApprovalEntity;
import com.g90.backend.modules.contract.entity.ContractApprovalStatus;
import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.contract.entity.ContractStatus;
import com.g90.backend.modules.contract.repository.ContractApprovalRepository;
import com.g90.backend.modules.contract.repository.ContractRepository;
import com.g90.backend.modules.dashboard.dto.DashboardQuery;
import com.g90.backend.modules.dashboard.dto.DashboardResponseData;
import com.g90.backend.modules.debt.repository.PaymentAllocationRepository;
import com.g90.backend.modules.payment.entity.InvoiceEntity;
import com.g90.backend.modules.payment.entity.PaymentConfirmationRequestEntity;
import com.g90.backend.modules.payment.repository.InvoiceRepository;
import com.g90.backend.modules.payment.repository.PaymentConfirmationRequestRepository;
import com.g90.backend.modules.project.entity.ProjectManagementEntity;
import com.g90.backend.modules.project.entity.ProjectMilestoneEntity;
import com.g90.backend.modules.project.entity.ProjectMilestoneStatus;
import com.g90.backend.modules.project.repository.ProjectMilestoneRepository;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String PENDING_PAYMENT_REVIEW = "PENDING_REVIEW";
    private static final Set<String> IGNORED_INVOICE_STATUSES = Set.of("DRAFT", "PAID", "SETTLED", "CLOSED", "CANCELLED", "VOID");
    private static final Set<String> WAREHOUSE_ACTION_STATUSES = Set.of(
            ContractStatus.SUBMITTED.name(),
            ContractStatus.PROCESSING.name(),
            ContractStatus.RESERVED.name(),
            ContractStatus.PICKED.name(),
            ContractStatus.IN_TRANSIT.name(),
            ContractStatus.DELIVERED.name()
    );

    private final ContractApprovalRepository contractApprovalRepository;
    private final PaymentConfirmationRequestRepository paymentConfirmationRequestRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final ContractRepository contractRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CurrentUserProvider currentUserProvider;

    public DashboardServiceImpl(
            ContractApprovalRepository contractApprovalRepository,
            PaymentConfirmationRequestRepository paymentConfirmationRequestRepository,
            InvoiceRepository invoiceRepository,
            PaymentAllocationRepository paymentAllocationRepository,
            ContractRepository contractRepository,
            ProjectMilestoneRepository projectMilestoneRepository,
            CustomerProfileRepository customerProfileRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.contractApprovalRepository = contractApprovalRepository;
        this.paymentConfirmationRequestRepository = paymentConfirmationRequestRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.contractRepository = contractRepository;
        this.projectMilestoneRepository = projectMilestoneRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponseData getDashboard(DashboardQuery query) {
        int limit = normalizeLimit(query);
        DashboardScope scope = resolveScope();

        List<DashboardResponseData.PendingApprovalItem> pendingApprovals = loadPendingApprovals(scope).stream()
                .map(this::toPendingApprovalItem)
                .toList();
        List<DashboardResponseData.PaymentConfirmationItem> pendingPaymentConfirmations = loadPendingPaymentConfirmations(scope).stream()
                .map(this::toPaymentConfirmationItem)
                .toList();
        List<DashboardResponseData.OverdueInvoiceItem> overdueInvoices = loadOverdueInvoices(scope).stream()
                .map(this::toOverdueInvoiceItem)
                .toList();
        List<DashboardResponseData.WarehouseActionItem> warehouseActions = loadWarehouseActions(scope).stream()
                .map(this::toWarehouseActionItem)
                .toList();
        List<DashboardResponseData.MilestoneConfirmationItem> milestoneConfirmations = loadMilestoneConfirmations(scope).stream()
                .map(this::toMilestoneConfirmationItem)
                .toList();

        DashboardResponseData.Summary summary = new DashboardResponseData.Summary(
                pendingApprovals.size(),
                pendingPaymentConfirmations.size(),
                overdueInvoices.size(),
                overdueInvoices.stream()
                        .map(DashboardResponseData.OverdueInvoiceItem::outstandingAmount)
                        .reduce(ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP),
                warehouseActions.size(),
                milestoneConfirmations.size()
        );

        return new DashboardResponseData(
                scope.role().name(),
                LocalDateTime.now(APP_ZONE),
                summary,
                limit(pendingApprovals, limit),
                limit(pendingPaymentConfirmations, limit),
                limit(overdueInvoices, limit),
                limit(warehouseActions, limit),
                limit(milestoneConfirmations, limit)
        );
    }

    private List<ContractApprovalEntity> loadPendingApprovals(DashboardScope scope) {
        if (scope.role() != RoleName.OWNER) {
            return List.of();
        }
        return contractApprovalRepository.findByStatusOrderByRequestedAtAsc(ContractApprovalStatus.PENDING.name());
    }

    private List<PaymentConfirmationRequestEntity> loadPendingPaymentConfirmations(DashboardScope scope) {
        if (scope.role() == RoleName.CUSTOMER) {
            return paymentConfirmationRequestRepository.findByCustomer_IdAndStatusOrderByCreatedAtAsc(
                    scope.customerId(),
                    PENDING_PAYMENT_REVIEW
            );
        }
        if (scope.role() == RoleName.OWNER || scope.role() == RoleName.ACCOUNTANT) {
            return paymentConfirmationRequestRepository.findByStatusOrderByCreatedAtAsc(PENDING_PAYMENT_REVIEW);
        }
        return List.of();
    }

    private List<InvoiceBalance> loadOverdueInvoices(DashboardScope scope) {
        if (scope.role() == RoleName.WAREHOUSE) {
            return List.of();
        }

        List<InvoiceEntity> invoices = scope.role() == RoleName.CUSTOMER
                ? invoiceRepository.findByCustomerIdWithCustomerAndContract(scope.customerId())
                : invoiceRepository.findAllWithCustomerAndContract();
        LocalDate today = LocalDate.now(APP_ZONE);
        return buildInvoiceBalances(invoices, today).stream()
                .filter(invoice -> invoice.invoice().getDueDate() != null)
                .filter(invoice -> invoice.invoice().getDueDate().isBefore(today))
                .filter(invoice -> invoice.outstandingAmount().compareTo(ZERO) > 0)
                .filter(invoice -> !IGNORED_INVOICE_STATUSES.contains(invoice.baseStatus()))
                .sorted(Comparator
                        .comparing((InvoiceBalance invoice) -> invoice.invoice().getDueDate())
                        .thenComparing(invoice -> invoice.invoice().getCreatedAt(), Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<ContractEntity> loadWarehouseActions(DashboardScope scope) {
        if (scope.role() == RoleName.CUSTOMER) {
            return sortWarehouseActions(contractRepository.findSaleOrdersByCustomerIdAndStatusIn(scope.customerId(), WAREHOUSE_ACTION_STATUSES));
        }
        if (scope.role() == RoleName.OWNER || scope.role() == RoleName.ACCOUNTANT || scope.role() == RoleName.WAREHOUSE) {
            return sortWarehouseActions(contractRepository.findSaleOrdersByStatusIn(WAREHOUSE_ACTION_STATUSES));
        }
        return List.of();
    }

    private List<ProjectMilestoneEntity> loadMilestoneConfirmations(DashboardScope scope) {
        if (scope.role() == RoleName.CUSTOMER) {
            return projectMilestoneRepository.findByProject_Customer_IdAndStatusOrderByDueDateAscCreatedAtAsc(
                    scope.customerId(),
                    ProjectMilestoneStatus.READY_FOR_CONFIRMATION.name()
            );
        }
        if (scope.role() == RoleName.OWNER || scope.role() == RoleName.ACCOUNTANT) {
            return projectMilestoneRepository.findByStatusOrderByDueDateAscCreatedAtAsc(ProjectMilestoneStatus.READY_FOR_CONFIRMATION.name());
        }
        return List.of();
    }

    private List<InvoiceBalance> buildInvoiceBalances(List<InvoiceEntity> invoices, LocalDate today) {
        if (invoices == null || invoices.isEmpty()) {
            return List.of();
        }

        Map<String, BigDecimal> allocatedMap = loadAllocatedMap(invoices);
        return invoices.stream()
                .map(invoice -> {
                    BigDecimal grandTotal = grandTotal(invoice);
                    BigDecimal paidAmount = normalizeMoney(allocatedMap.getOrDefault(invoice.getId(), ZERO)).min(grandTotal);
                    BigDecimal outstandingAmount = grandTotal.subtract(paidAmount).max(ZERO).setScale(2, RoundingMode.HALF_UP);
                    long overdueDays = invoice.getDueDate() == null || !invoice.getDueDate().isBefore(today)
                            ? 0L
                            : ChronoUnit.DAYS.between(invoice.getDueDate(), today);
                    String baseStatus = normalizeUpper(invoice.getStatus());
                    return new InvoiceBalance(
                            invoice,
                            grandTotal,
                            paidAmount,
                            outstandingAmount,
                            deriveInvoiceStatus(baseStatus, paidAmount, grandTotal, overdueDays),
                            baseStatus,
                            overdueDays
                    );
                })
                .toList();
    }

    private Map<String, BigDecimal> loadAllocatedMap(Collection<InvoiceEntity> invoices) {
        List<String> invoiceIds = invoices.stream()
                .map(InvoiceEntity::getId)
                .filter(StringUtils::hasText)
                .toList();
        if (invoiceIds.isEmpty()) {
            return Map.of();
        }

        Map<String, BigDecimal> allocatedMap = new LinkedHashMap<>();
        for (PaymentAllocationRepository.InvoiceAllocationTotalView view : paymentAllocationRepository.summarizeByInvoiceIds(invoiceIds)) {
            allocatedMap.put(view.getInvoiceId(), normalizeMoney(view.getAllocatedAmount()));
        }
        return allocatedMap;
    }

    private DashboardResponseData.PendingApprovalItem toPendingApprovalItem(ContractApprovalEntity approval) {
        ContractEntity contract = approval.getContract();
        CustomerProfileEntity customer = contract == null ? null : contract.getCustomer();
        return new DashboardResponseData.PendingApprovalItem(
                approval.getId(),
                contract == null ? null : contract.getId(),
                contract == null ? null : contract.getContractNumber(),
                customer == null ? null : customer.getId(),
                customer == null ? null : customer.getCompanyName(),
                contract == null ? ZERO : normalizeMoney(contract.getTotalAmount()),
                approval.getApprovalType(),
                approval.getApprovalTier(),
                contract == null ? null : contract.getPendingAction(),
                approval.getRequestedBy(),
                approval.getRequestedAt(),
                approval.getDueAt(),
                approval.getStatus()
        );
    }

    private DashboardResponseData.PaymentConfirmationItem toPaymentConfirmationItem(PaymentConfirmationRequestEntity request) {
        InvoiceEntity invoice = request.getInvoice();
        CustomerProfileEntity customer = request.getCustomer() != null ? request.getCustomer() : invoice == null ? null : invoice.getCustomer();
        return new DashboardResponseData.PaymentConfirmationItem(
                request.getId(),
                invoice == null ? null : invoice.getId(),
                invoice == null ? null : invoice.getInvoiceNumber(),
                customer == null ? null : customer.getId(),
                customer == null ? null : customer.getCustomerCode(),
                resolveCustomerName(customer, invoice),
                normalizeMoney(request.getRequestedAmount()),
                request.getTransferTime(),
                request.getSenderBankName(),
                request.getReferenceCode(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }

    private DashboardResponseData.OverdueInvoiceItem toOverdueInvoiceItem(InvoiceBalance invoiceBalance) {
        InvoiceEntity invoice = invoiceBalance.invoice();
        CustomerProfileEntity customer = invoice.getCustomer();
        ContractEntity contract = invoice.getContract();
        return new DashboardResponseData.OverdueInvoiceItem(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                contract == null ? null : contract.getId(),
                contract == null ? null : contract.getContractNumber(),
                customer == null ? null : customer.getId(),
                customer == null ? null : customer.getCustomerCode(),
                resolveCustomerName(customer, invoice),
                invoice.getDueDate(),
                invoiceBalance.overdueDays(),
                invoiceBalance.grandTotal(),
                invoiceBalance.paidAmount(),
                invoiceBalance.outstandingAmount(),
                invoiceBalance.status()
        );
    }

    private DashboardResponseData.WarehouseActionItem toWarehouseActionItem(ContractEntity contract) {
        CustomerProfileEntity customer = contract.getCustomer();
        return new DashboardResponseData.WarehouseActionItem(
                contract.getId(),
                contract.getSaleOrderNumber(),
                contract.getContractNumber(),
                customer == null ? null : customer.getId(),
                customer == null ? null : customer.getCompanyName(),
                contract.getExpectedDeliveryDate(),
                contract.getActualDeliveryDate(),
                contract.getStatus(),
                normalizeMoney(contract.getTotalAmount()),
                contract.getSubmittedAt()
        );
    }

    private DashboardResponseData.MilestoneConfirmationItem toMilestoneConfirmationItem(ProjectMilestoneEntity milestone) {
        ProjectManagementEntity project = milestone.getProject();
        CustomerProfileEntity customer = project == null ? null : project.getCustomer();
        return new DashboardResponseData.MilestoneConfirmationItem(
                milestone.getId(),
                project == null ? null : project.getId(),
                project == null ? null : project.getProjectCode(),
                project == null ? null : project.getName(),
                customer == null ? null : customer.getId(),
                customer == null ? null : customer.getCompanyName(),
                milestone.getName(),
                milestone.getCompletionPercent(),
                normalizeMoney(milestone.getAmount()),
                milestone.getDueDate(),
                milestone.getConfirmationDeadline(),
                milestone.getStatus(),
                milestone.getConfirmationStatus()
        );
    }

    private List<ContractEntity> sortWarehouseActions(List<ContractEntity> contracts) {
        return contracts.stream()
                .sorted(Comparator
                        .comparing(ContractEntity::getExpectedDeliveryDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ContractEntity::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ContractEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private DashboardScope resolveScope() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        RoleName role = roleOf(currentUser);
        CustomerProfileEntity customer = role == RoleName.CUSTOMER
                ? customerProfileRepository.findByUser_Id(currentUser.userId()).orElseThrow(CustomerProfileNotFoundException::new)
                : null;
        return new DashboardScope(role, customer);
    }

    private RoleName roleOf(AuthenticatedUser currentUser) {
        try {
            return RoleName.from(currentUser.role());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ForbiddenOperationException("Invalid user role");
        }
    }

    private int normalizeLimit(DashboardQuery query) {
        if (query == null || query.getLimit() == null) {
            return 10;
        }
        return Math.max(1, Math.min(query.getLimit(), 50));
    }

    private <T> List<T> limit(List<T> items, int limit) {
        if (items.size() <= limit) {
            return items;
        }
        return items.subList(0, limit);
    }

    private BigDecimal grandTotal(InvoiceEntity invoice) {
        return normalizeMoney(invoice.getTotalAmount()).add(normalizeMoney(invoice.getVatAmount())).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String deriveInvoiceStatus(String baseStatus, BigDecimal paidAmount, BigDecimal grandTotal, long overdueDays) {
        if (IGNORED_INVOICE_STATUSES.contains(baseStatus)) {
            return baseStatus;
        }
        if (grandTotal.compareTo(ZERO) > 0 && paidAmount.compareTo(grandTotal) >= 0) {
            return "PAID";
        }
        if (overdueDays > 0) {
            return "OVERDUE";
        }
        if (paidAmount.compareTo(ZERO) > 0) {
            return "PARTIALLY_PAID";
        }
        return StringUtils.hasText(baseStatus) ? baseStatus : "ISSUED";
    }

    private String resolveCustomerName(CustomerProfileEntity customer, InvoiceEntity invoice) {
        if (customer != null && StringUtils.hasText(customer.getCompanyName())) {
            return customer.getCompanyName();
        }
        return invoice == null ? null : invoice.getCustomerName();
    }

    private record DashboardScope(RoleName role, CustomerProfileEntity customer) {

        private String customerId() {
            return customer == null ? null : customer.getId();
        }
    }

    private record InvoiceBalance(
            InvoiceEntity invoice,
            BigDecimal grandTotal,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,
            String status,
            String baseStatus,
            long overdueDays
    ) {
    }
}
