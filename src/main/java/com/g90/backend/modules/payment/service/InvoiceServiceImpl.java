package com.g90.backend.modules.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.CustomerProfileNotFoundException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.InvoiceNotFoundException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.contract.entity.ContractItemEntity;
import com.g90.backend.modules.contract.repository.ContractRepository;
import com.g90.backend.modules.debt.entity.PaymentAllocationEntity;
import com.g90.backend.modules.debt.repository.PaymentAllocationRepository;
import com.g90.backend.modules.email.service.EmailService;
import com.g90.backend.modules.payment.dto.InvoiceCancelRequest;
import com.g90.backend.modules.payment.dto.InvoiceCreateRequest;
import com.g90.backend.modules.payment.dto.InvoiceItemRequest;
import com.g90.backend.modules.payment.dto.InvoiceListQuery;
import com.g90.backend.modules.payment.dto.InvoiceListResponseData;
import com.g90.backend.modules.payment.dto.InvoiceResponse;
import com.g90.backend.modules.payment.dto.InvoiceUpdateRequest;
import com.g90.backend.modules.payment.entity.InvoiceEntity;
import com.g90.backend.modules.payment.entity.InvoiceItemEntity;
import com.g90.backend.modules.payment.repository.InvoiceRepository;
import com.g90.backend.modules.product.dto.PaginationResponse;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal VAT_WITH_TAX_CODE = new BigDecimal("10.00").setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal UPDATE_APPROVAL_THRESHOLD = new BigDecimal("1.05");
    private static final Set<String> ALLOWED_CREATE_STATUSES = Set.of("DRAFT", "ISSUED");
    private static final Set<String> INVOICE_ELIGIBLE_CONTRACT_STATUSES = Set.of("DELIVERED", "COMPLETED");
    private static final Set<String> UPDATABLE_INVOICE_STATUSES = Set.of("DRAFT", "ISSUED");
    private static final Set<String> LOCKED_INVOICE_STATUSES = Set.of("PARTIALLY_PAID", "PAID", "SETTLED", "CANCELLED", "VOID");
    private static final Set<String> ALLOWED_LIST_STATUSES = Set.of("DRAFT", "ISSUED", "PARTIALLY_PAID", "PAID", "CANCELLED", "VOID");
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("invoiceNumber", "customerName", "issueDate", "dueDate", "grandTotal", "outstandingAmount", "status");
    private static final Set<String> ALLOWED_SORT_DIRECTIONS = Set.of("asc", "desc");

    private final InvoiceRepository invoiceRepository;
    private final ContractRepository contractRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            ContractRepository contractRepository,
            CustomerProfileRepository customerProfileRepository,
            PaymentAllocationRepository paymentAllocationRepository,
            CurrentUserProvider currentUserProvider,
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper,
            EmailService emailService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.contractRepository = contractRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public InvoiceResponse createInvoice(InvoiceCreateRequest request) {
        AuthenticatedUser currentUser = requireInvoiceManager();
        normalizeAndValidateCreateRequest(request);

        ContractEntity contract = loadBillableContract(request.getContractId());
        if (invoiceRepository.existsByContract_IdAndStatusNotIn(contract.getId(), List.of("CANCELLED", "VOID"))) {
            throw RequestValidationException.singleError("contractId", "Contract already has an active invoice");
        }

        CustomerProfileEntity customer = contract.getCustomer();
        List<ResolvedInvoiceItem> items = request.getItems() == null
                ? resolveContractItems(contract)
                : resolveRequestedItems(contract, request.getItems());
        InvoiceAmounts amounts = computeInvoiceAmounts(customer, items, request.getAdjustmentAmount());

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber(generateInvoiceNumber(request.getIssueDate()));
        invoice.setContract(contract);
        invoice.setCustomer(customer);
        invoice.setSourceType("CONTRACT");
        invoice.setCustomerName(normalizeNullable(customer.getCompanyName()));
        invoice.setCustomerTaxCode(normalizeNullable(customer.getTaxCode()));
        invoice.setBillingAddress(firstNonBlank(request.getBillingAddress(), customer.getAddress()));
        invoice.setIssueDate(request.getIssueDate());
        invoice.setPaymentTerms(firstNonBlank(request.getPaymentTerms(), contract.getPaymentTerms(), customer.getPaymentTerms()));
        invoice.setNote(request.getNote());
        invoice.setAdjustmentAmount(amounts.adjustmentAmount());
        invoice.setTotalAmount(amounts.totalAmount());
        invoice.setVatRate(amounts.vatRate());
        invoice.setVatAmount(amounts.vatAmount());
        invoice.setDueDate(request.getDueDate());
        invoice.setStatus(request.getStatus());
        invoice.setCreatedBy(currentUser.userId());
        invoice.setUpdatedBy(currentUser.userId());
        if ("ISSUED".equals(request.getStatus())) {
            invoice.setIssuedBy(currentUser.userId());
            invoice.setIssuedAt(LocalDateTime.now(APP_ZONE));
        }
        replaceInvoiceItems(invoice, items);

        InvoiceEntity saved = invoiceRepository.save(invoice);
        if ("ISSUED".equals(saved.getStatus())) {
            logAudit("ISSUE_INVOICE", "INVOICE", saved.getId(), null, auditPayload(saved), currentUser.userId());
            saved = maybeSendInvoiceNotification(saved, currentUser, "invoice-issued", "Invoice issued - " + saved.getInvoiceNumber(), "NOTIFY_INVOICE_CUSTOMER_ISSUE");
        }

        InvoiceResponse response = toInvoiceResponse(saved, buildPaymentSummaries(List.of(saved)).get(saved.getId()), List.of());
        logAudit("CREATE_INVOICE", "INVOICE", saved.getId(), null, response, currentUser.userId());
        return response;
    }

    @Override
    @Transactional
    public InvoiceListResponseData getInvoices(InvoiceListQuery query) {
        AuthenticatedUser currentUser = requireInvoiceViewer();
        normalizeAndValidateListQuery(query);

        List<InvoiceEntity> invoices = loadAccessibleInvoices(currentUser);
        Map<String, InvoicePaymentSummary> paymentSummaries = buildPaymentSummaries(invoices);
        List<InvoiceSummaryView> matched = invoices.stream()
                .map(invoice -> new InvoiceSummaryView(invoice, paymentSummaries.getOrDefault(invoice.getId(), emptyPaymentSummary(normalizeStoredStatus(invoice.getStatus())))))
                .filter(view -> matchesQuery(view, query))
                .sorted(buildComparator(query))
                .toList();

        int page = query.getPage();
        int pageSize = query.getPageSize();
        int fromIndex = Math.min((page - 1) * pageSize, matched.size());
        int toIndex = Math.min(fromIndex + pageSize, matched.size());
        int totalPages = matched.isEmpty() ? 0 : (int) Math.ceil((double) matched.size() / pageSize);

        return new InvoiceListResponseData(
                matched.subList(fromIndex, toIndex).stream().map(this::toInvoiceListItem).toList(),
                PaginationResponse.builder()
                        .page(page)
                        .pageSize(pageSize)
                        .totalItems(matched.size())
                        .totalPages(totalPages)
                        .build(),
                new InvoiceListResponseData.Filters(
                        query.getKeyword(),
                        query.getInvoiceNumber(),
                        query.getCustomerId(),
                        query.getCustomerName(),
                        query.getContractId(),
                        query.getStatus()
                )
        );
    }

    @Override
    @Transactional
    public InvoiceResponse getInvoice(String invoiceId) {
        AuthenticatedUser currentUser = requireInvoiceViewer();
        InvoiceEntity invoice = loadAccessibleInvoice(invoiceId, currentUser);
        InvoicePaymentSummary paymentSummary = buildPaymentSummaries(List.of(invoice)).get(invoice.getId());
        return toInvoiceResponse(invoice, paymentSummary, loadPaymentHistory(invoice.getId()));
    }

    @Override
    @Transactional
    public InvoiceResponse updateInvoice(String invoiceId, InvoiceUpdateRequest request) {
        AuthenticatedUser currentUser = requireInvoiceManager();
        normalizeAndValidateUpdateRequest(request);
        InvoiceEntity invoice = invoiceRepository.findDetailedById(invoiceId).orElseThrow(InvoiceNotFoundException::new);
        InvoicePaymentSummary existingSummary = buildPaymentSummaries(List.of(invoice)).get(invoice.getId());
        String currentStatus = existingSummary.status();
        if (!UPDATABLE_INVOICE_STATUSES.contains(currentStatus) || LOCKED_INVOICE_STATUSES.contains(currentStatus)) {
            throw RequestValidationException.singleError("invoiceId", "Only draft or issued invoices can be updated before payment");
        }
        if (existingSummary.paidAmount().compareTo(ZERO) > 0) {
            throw RequestValidationException.singleError("invoiceId", "Invoice with recorded payment cannot be updated");
        }

        InvoiceResponse oldState = toInvoiceResponse(invoice, existingSummary, List.of());
        ContractEntity contract = invoice.getContract() == null
                ? null
                : contractRepository.findDetailedById(invoice.getContract().getId()).orElse(invoice.getContract());
        CustomerProfileEntity customer = invoice.getCustomer();
        LocalDate requestedIssueDate = request.getIssueDate() != null
                ? request.getIssueDate()
                : firstNonNull(invoice.getIssueDate(), invoice.getCreatedAt() == null ? LocalDate.now(APP_ZONE) : invoice.getCreatedAt().toLocalDate());
        LocalDate requestedDueDate = request.getDueDate() != null ? request.getDueDate() : invoice.getDueDate();
        if (requestedDueDate == null) {
            throw RequestValidationException.singleError("dueDate", "dueDate is required");
        }
        if (requestedDueDate.isBefore(requestedIssueDate)) {
            throw RequestValidationException.singleError("dueDate", "dueDate must be on or after issueDate");
        }
        if ("ISSUED".equals(currentStatus) && request.getIssueDate() != null && !requestedIssueDate.equals(invoice.getIssueDate())) {
            throw RequestValidationException.singleError("issueDate", "Issued invoice date cannot be changed");
        }
        if ("ISSUED".equals(currentStatus) && request.getDueDate() != null && invoice.getDueDate() != null && requestedDueDate.isBefore(invoice.getDueDate())) {
            throw RequestValidationException.singleError("dueDate", "Payment terms cannot be shortened after invoice issue");
        }

        String nextStatus = resolveUpdateStatus(currentStatus, request.getStatus());
        List<ResolvedInvoiceItem> items = request.getItems() == null ? snapshotItems(invoice) : resolveRequestedItems(contract, request.getItems());
        InvoiceAmounts amounts = computeInvoiceAmounts(
                customer,
                items,
                request.getAdjustmentAmount() != null ? normalizeMoney(request.getAdjustmentAmount()) : normalizeMoney(invoice.getAdjustmentAmount())
        );
        BigDecimal previousGrandTotal = grandTotal(invoice.getTotalAmount(), invoice.getVatAmount());
        if (amounts.grandTotal().compareTo(previousGrandTotal.multiply(UPDATE_APPROVAL_THRESHOLD).setScale(2, RoundingMode.HALF_UP)) > 0
                && roleOf(currentUser) != RoleName.OWNER) {
            throw RequestValidationException.singleError("totalAmount", "Amount increases greater than 5% require finance approval");
        }

        applyInvoiceUpdates(invoice, request, customer, currentUser, requestedIssueDate, requestedDueDate, nextStatus, amounts, items);
        InvoiceEntity saved = invoiceRepository.save(invoice);
        if ("DRAFT".equals(currentStatus) && "ISSUED".equals(nextStatus)) {
            logAudit("ISSUE_INVOICE", "INVOICE", saved.getId(), auditPayload(oldState), auditPayload(saved), currentUser.userId());
        }
        if ("ISSUED".equals(nextStatus)) {
            saved = maybeSendInvoiceNotification(saved, currentUser, "invoice-updated", "Invoice updated - " + saved.getInvoiceNumber(), "NOTIFY_INVOICE_CUSTOMER_UPDATE");
        }

        InvoiceResponse response = toInvoiceResponse(saved, buildPaymentSummaries(List.of(saved)).get(saved.getId()), List.of());
        logAudit("UPDATE_INVOICE", "INVOICE", saved.getId(), oldState, response, currentUser.userId());
        return response;
    }

    @Override
    @Transactional
    public InvoiceResponse cancelInvoice(String invoiceId, InvoiceCancelRequest request) {
        AuthenticatedUser currentUser = requireFinanceManager();
        request.setCancellationReason(normalizeRequired(request.getCancellationReason(), "cancellationReason", "cancellationReason is required"));
        InvoiceEntity invoice = invoiceRepository.findDetailedById(invoiceId).orElseThrow(InvoiceNotFoundException::new);
        InvoicePaymentSummary summary = buildPaymentSummaries(List.of(invoice)).get(invoice.getId());
        String currentStatus = summary.status();
        if ("CANCELLED".equals(currentStatus) || "VOID".equals(currentStatus)) {
            throw RequestValidationException.singleError("invoiceId", "Invoice is already cancelled");
        }
        if ("PAID".equals(currentStatus) || "PARTIALLY_PAID".equals(currentStatus) || "SETTLED".equals(currentStatus) || summary.paidAmount().compareTo(ZERO) > 0) {
            throw RequestValidationException.singleError("invoiceId", "Refund is required before cancelling an invoice with received payment");
        }

        InvoiceResponse oldState = toInvoiceResponse(invoice, summary, List.of());
        invoice.setStatus("CANCELLED");
        invoice.setCancellationReason(request.getCancellationReason());
        invoice.setCancelledBy(currentUser.userId());
        invoice.setCancelledAt(LocalDateTime.now(APP_ZONE));
        invoice.setUpdatedBy(currentUser.userId());

        InvoiceEntity saved = invoiceRepository.save(invoice);
        saved = maybeSendInvoiceNotification(saved, currentUser, "invoice-cancelled", "Invoice cancelled - " + saved.getInvoiceNumber(), "NOTIFY_INVOICE_CUSTOMER_CANCEL");
        InvoiceResponse response = toInvoiceResponse(saved, buildPaymentSummaries(List.of(saved)).get(saved.getId()), List.of());
        logAudit("CANCEL_INVOICE", "INVOICE", saved.getId(), oldState, response, currentUser.userId());
        return response;
    }

    private void applyInvoiceUpdates(
            InvoiceEntity invoice,
            InvoiceUpdateRequest request,
            CustomerProfileEntity customer,
            AuthenticatedUser currentUser,
            LocalDate requestedIssueDate,
            LocalDate requestedDueDate,
            String nextStatus,
            InvoiceAmounts amounts,
            List<ResolvedInvoiceItem> items
    ) {
        invoice.setCustomerName(normalizeNullable(customer == null ? null : customer.getCompanyName()));
        invoice.setCustomerTaxCode(normalizeNullable(customer == null ? null : customer.getTaxCode()));
        if (request.getBillingAddress() != null) {
            invoice.setBillingAddress(normalizeNullable(request.getBillingAddress()));
        }
        if (request.getPaymentTerms() != null) {
            invoice.setPaymentTerms(normalizeNullable(request.getPaymentTerms()));
        }
        if (request.getNote() != null) {
            invoice.setNote(normalizeNullable(request.getNote()));
        }
        if (request.getDocumentUrl() != null) {
            invoice.setDocumentUrl(normalizeNullable(request.getDocumentUrl()));
        }
        invoice.setIssueDate(requestedIssueDate);
        invoice.setDueDate(requestedDueDate);
        invoice.setAdjustmentAmount(amounts.adjustmentAmount());
        invoice.setTotalAmount(amounts.totalAmount());
        invoice.setVatRate(amounts.vatRate());
        invoice.setVatAmount(amounts.vatAmount());
        invoice.setStatus(nextStatus);
        invoice.setUpdatedBy(currentUser.userId());
        if ("ISSUED".equals(nextStatus) && invoice.getIssuedAt() == null) {
            invoice.setIssuedAt(LocalDateTime.now(APP_ZONE));
            invoice.setIssuedBy(currentUser.userId());
        }
        if (request.getItems() != null) {
            replaceInvoiceItems(invoice, items);
        }
    }

    private List<InvoiceEntity> loadAccessibleInvoices(AuthenticatedUser currentUser) {
        RoleName role = roleOf(currentUser);
        if (role == RoleName.CUSTOMER) {
            CustomerProfileEntity customer = loadCustomerForCurrentUser(currentUser);
            return invoiceRepository.findByCustomerIdWithCustomerAndContract(customer.getId());
        }
        if (role != RoleName.ACCOUNTANT && role != RoleName.OWNER) {
            throw new ForbiddenOperationException("You do not have permission to view invoices");
        }
        return invoiceRepository.findAllWithCustomerAndContract();
    }

    private InvoiceEntity loadAccessibleInvoice(String invoiceId, AuthenticatedUser currentUser) {
        RoleName role = roleOf(currentUser);
        if (role == RoleName.CUSTOMER) {
            CustomerProfileEntity customer = loadCustomerForCurrentUser(currentUser);
            return invoiceRepository.findDetailedByIdAndCustomerId(invoiceId, customer.getId()).orElseThrow(InvoiceNotFoundException::new);
        }
        if (role != RoleName.ACCOUNTANT && role != RoleName.OWNER) {
            throw new ForbiddenOperationException("You do not have permission to view invoices");
        }
        return invoiceRepository.findDetailedById(invoiceId).orElseThrow(InvoiceNotFoundException::new);
    }

    private ContractEntity loadBillableContract(String contractId) {
        ContractEntity contract = contractRepository.findDetailedById(contractId)
                .orElseThrow(() -> RequestValidationException.singleError("contractId", "Contract not found"));
        if (contract.getCustomer() == null) {
            throw RequestValidationException.singleError("contractId", "Contract is missing customer information");
        }
        if (!INVOICE_ELIGIBLE_CONTRACT_STATUSES.contains(normalizeUpper(contract.getStatus()))) {
            throw RequestValidationException.singleError("contractId", "Invoice can only be created from a delivered or completed sale order");
        }
        return contract;
    }

    private List<ResolvedInvoiceItem> resolveContractItems(ContractEntity contract) {
        if (contract.getItems() == null || contract.getItems().isEmpty()) {
            throw RequestValidationException.singleError("contractId", "Contract does not contain billable items");
        }
        List<ResolvedInvoiceItem> items = new ArrayList<>();
        int index = 0;
        for (ContractItemEntity contractItem : contract.getItems()) {
            ProductEntity product = contractItem.getProduct();
            BigDecimal quantity = normalizePositiveMoney(contractItem.getQuantity(), "items[" + index + "].quantity", "Contract item quantity must be greater than 0");
            BigDecimal totalPrice = normalizeMoney(contractItem.getTotalPrice());
            BigDecimal unitPrice = normalizeMoney(contractItem.getUnitPrice());
            if (unitPrice.compareTo(ZERO) <= 0) {
                if (totalPrice.compareTo(ZERO) <= 0) {
                    throw RequestValidationException.singleError("items[" + index + "].unitPrice", "Contract item unitPrice must be greater than 0");
                }
                unitPrice = totalPrice.divide(quantity, 2, RoundingMode.HALF_UP);
            }
            if (totalPrice.compareTo(ZERO) <= 0) {
                totalPrice = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            }
            items.add(new ResolvedInvoiceItem(
                    product,
                    firstNonBlank(product == null ? null : product.getProductName(), "Contract item"),
                    firstNonBlank(product == null ? null : product.getUnit(), "ITEM"),
                    quantity,
                    unitPrice,
                    totalPrice
            ));
            index++;
        }
        return items;
    }

    private List<ResolvedInvoiceItem> resolveRequestedItems(ContractEntity contract, List<InvoiceItemRequest> requestedItems) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw RequestValidationException.singleError("items", "Invoice must contain at least one item");
        }

        Map<String, ProductEntity> contractProducts = new LinkedHashMap<>();
        if (contract != null && contract.getItems() != null) {
            for (ContractItemEntity item : contract.getItems()) {
                if (item.getProduct() != null && StringUtils.hasText(item.getProduct().getId())) {
                    contractProducts.put(item.getProduct().getId(), item.getProduct());
                }
            }
        }

        List<ResolvedInvoiceItem> items = new ArrayList<>();
        for (int i = 0; i < requestedItems.size(); i++) {
            InvoiceItemRequest itemRequest = requestedItems.get(i);
            String productId = normalizeNullable(itemRequest.getProductId());
            ProductEntity product = null;
            if (StringUtils.hasText(productId)) {
                product = contractProducts.get(productId);
                if (product == null) {
                    throw RequestValidationException.singleError("items[" + i + "].productId", "Item productId must belong to the selected contract");
                }
            }
            BigDecimal quantity = normalizePositiveMoney(itemRequest.getQuantity(), "items[" + i + "].quantity", "quantity must be greater than 0");
            BigDecimal unitPrice = normalizePositiveMoney(itemRequest.getUnitPrice(), "items[" + i + "].unitPrice", "unitPrice must be greater than 0");
            String description = firstNonBlank(normalizeNullable(itemRequest.getDescription()), product == null ? null : product.getProductName());
            if (!StringUtils.hasText(description)) {
                throw RequestValidationException.singleError("items[" + i + "].description", "description is required");
            }
            String unit = firstNonBlank(normalizeNullable(itemRequest.getUnit()), product == null ? null : product.getUnit());
            if (!StringUtils.hasText(unit)) {
                throw RequestValidationException.singleError("items[" + i + "].unit", "unit is required");
            }
            items.add(new ResolvedInvoiceItem(
                    product,
                    description,
                    unit,
                    quantity,
                    unitPrice,
                    quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP)
            ));
        }
        return items;
    }

    private List<ResolvedInvoiceItem> snapshotItems(InvoiceEntity invoice) {
        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            throw RequestValidationException.singleError("items", "Existing invoice does not contain billable items");
        }
        return invoice.getItems().stream()
                .map(item -> new ResolvedInvoiceItem(
                        item.getProduct(),
                        firstNonBlank(item.getDescription(), item.getProduct() == null ? null : item.getProduct().getProductName(), "Invoice item"),
                        firstNonBlank(item.getUnit(), item.getProduct() == null ? null : item.getProduct().getUnit(), "ITEM"),
                        normalizePositiveMoney(item.getQuantity(), "items.quantity", "quantity must be greater than 0"),
                        normalizePositiveMoney(item.getUnitPrice(), "items.unitPrice", "unitPrice must be greater than 0"),
                        normalizePositiveMoney(item.getTotalPrice(), "items.totalPrice", "totalPrice must be greater than 0")
                ))
                .toList();
    }

    private void replaceInvoiceItems(InvoiceEntity invoice, List<ResolvedInvoiceItem> items) {
        invoice.getItems().clear();
        for (ResolvedInvoiceItem item : items) {
            InvoiceItemEntity entity = new InvoiceItemEntity();
            entity.setInvoice(invoice);
            entity.setProduct(item.product());
            entity.setDescription(item.description());
            entity.setUnit(item.unit());
            entity.setQuantity(item.quantity());
            entity.setUnitPrice(item.unitPrice());
            entity.setTotalPrice(item.totalPrice());
            invoice.getItems().add(entity);
        }
    }

    private Map<String, InvoicePaymentSummary> buildPaymentSummaries(Collection<InvoiceEntity> invoices) {
        Map<String, InvoicePaymentSummary> summaries = new LinkedHashMap<>();
        if (invoices == null || invoices.isEmpty()) {
            return summaries;
        }

        List<String> invoiceIds = invoices.stream().map(InvoiceEntity::getId).toList();
        Map<String, BigDecimal> allocatedMap = new LinkedHashMap<>();
        for (PaymentAllocationRepository.InvoiceAllocationTotalView view : paymentAllocationRepository.summarizeByInvoiceIds(invoiceIds)) {
            allocatedMap.put(view.getInvoiceId(), normalizeMoney(view.getAllocatedAmount()));
        }

        for (InvoiceEntity invoice : invoices) {
            BigDecimal grandTotal = grandTotal(invoice.getTotalAmount(), invoice.getVatAmount());
            BigDecimal paidAmount = normalizeMoney(allocatedMap.getOrDefault(invoice.getId(), ZERO));
            if (paidAmount.compareTo(grandTotal) > 0) {
                paidAmount = grandTotal;
            }
            BigDecimal outstandingAmount = grandTotal.subtract(paidAmount).setScale(2, RoundingMode.HALF_UP);
            if (outstandingAmount.compareTo(ZERO) < 0) {
                outstandingAmount = ZERO;
            }
            summaries.put(invoice.getId(), new InvoicePaymentSummary(
                    paidAmount,
                    outstandingAmount,
                    deriveDisplayStatus(invoice.getStatus(), paidAmount, grandTotal)
            ));
        }
        return summaries;
    }

    private List<InvoiceResponse.PaymentHistoryItem> loadPaymentHistory(String invoiceId) {
        return paymentAllocationRepository.findDetailedByInvoiceIds(List.of(invoiceId)).stream()
                .map(this::toPaymentHistoryItem)
                .toList();
    }

    private InvoiceResponse.PaymentHistoryItem toPaymentHistoryItem(PaymentAllocationEntity allocation) {
        return new InvoiceResponse.PaymentHistoryItem(
                allocation.getPayment().getId(),
                buildReceiptNumber(allocation.getPayment().getId()),
                allocation.getPayment().getPaymentDate(),
                normalizeMoney(allocation.getAmount()),
                normalizeMoney(allocation.getPayment().getAmount()),
                normalizeUpper(allocation.getPayment().getPaymentMethod()),
                normalizeNullable(allocation.getPayment().getReferenceNo()),
                normalizeNullable(allocation.getPayment().getNote()),
                allocation.getPayment().getCreatedBy(),
                allocation.getPayment().getCreatedAt()
        );
    }

    private InvoiceResponse toInvoiceResponse(
            InvoiceEntity invoice,
            InvoicePaymentSummary paymentSummary,
            List<InvoiceResponse.PaymentHistoryItem> paymentHistory
    ) {
        List<InvoiceResponse.Item> items = invoice.getItems() == null
                ? List.of()
                : invoice.getItems().stream()
                .map(item -> new InvoiceResponse.Item(
                        item.getId(),
                        item.getProduct() == null ? null : item.getProduct().getId(),
                        item.getProduct() == null ? null : item.getProduct().getProductCode(),
                        item.getProduct() == null ? null : item.getProduct().getProductName(),
                        firstNonBlank(item.getDescription(), item.getProduct() == null ? null : item.getProduct().getProductName()),
                        firstNonBlank(item.getUnit(), item.getProduct() == null ? null : item.getProduct().getUnit()),
                        normalizeMoney(item.getQuantity()),
                        normalizeMoney(item.getUnitPrice()),
                        normalizeMoney(item.getTotalPrice())
                ))
                .toList();
        BigDecimal subtotalAmount = items.isEmpty()
                ? normalizeMoney(invoice.getTotalAmount()).subtract(normalizeMoney(invoice.getAdjustmentAmount())).setScale(2, RoundingMode.HALF_UP)
                : items.stream().map(InvoiceResponse.Item::totalPrice).reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        if (subtotalAmount.compareTo(ZERO) < 0) {
            subtotalAmount = ZERO;
        }

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                normalizeUpper(invoice.getSourceType()),
                invoice.getContract() == null ? null : invoice.getContract().getId(),
                invoice.getContract() == null ? null : invoice.getContract().getContractNumber(),
                invoice.getCustomer() == null ? null : invoice.getCustomer().getId(),
                invoice.getCustomer() == null ? null : invoice.getCustomer().getCustomerCode(),
                firstNonBlank(invoice.getCustomerName(), invoice.getCustomer() == null ? null : invoice.getCustomer().getCompanyName()),
                firstNonBlank(invoice.getCustomerTaxCode(), invoice.getCustomer() == null ? null : invoice.getCustomer().getTaxCode()),
                invoice.getBillingAddress(),
                invoice.getPaymentTerms(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                subtotalAmount,
                normalizeMoney(invoice.getAdjustmentAmount()),
                normalizeMoney(invoice.getTotalAmount()),
                normalizeMoney(invoice.getVatRate()),
                normalizeMoney(invoice.getVatAmount()),
                grandTotal(invoice.getTotalAmount(), invoice.getVatAmount()),
                paymentSummary == null ? ZERO : paymentSummary.paidAmount(),
                paymentSummary == null ? grandTotal(invoice.getTotalAmount(), invoice.getVatAmount()) : paymentSummary.outstandingAmount(),
                paymentSummary == null ? normalizeStoredStatus(invoice.getStatus()) : paymentSummary.status(),
                invoice.getNote(),
                invoice.getDocumentUrl(),
                invoice.getCancellationReason(),
                invoice.getCreatedBy(),
                invoice.getUpdatedBy(),
                invoice.getIssuedBy(),
                invoice.getCancelledBy(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt(),
                invoice.getIssuedAt(),
                invoice.getCancelledAt(),
                invoice.getNotificationSentAt(),
                items,
                paymentHistory == null ? List.of() : paymentHistory
        );
    }

    private InvoiceListResponseData.Item toInvoiceListItem(InvoiceSummaryView view) {
        InvoiceEntity invoice = view.invoice();
        return new InvoiceListResponseData.Item(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                normalizeUpper(invoice.getSourceType()),
                invoice.getContract() == null ? null : invoice.getContract().getId(),
                invoice.getContract() == null ? null : invoice.getContract().getContractNumber(),
                invoice.getCustomer() == null ? null : invoice.getCustomer().getId(),
                invoice.getCustomer() == null ? null : invoice.getCustomer().getCustomerCode(),
                firstNonBlank(invoice.getCustomerName(), invoice.getCustomer() == null ? null : invoice.getCustomer().getCompanyName()),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                grandTotal(invoice.getTotalAmount(), invoice.getVatAmount()),
                view.paymentSummary().paidAmount(),
                view.paymentSummary().outstandingAmount(),
                view.paymentSummary().status(),
                invoice.getDocumentUrl()
        );
    }

    private boolean matchesQuery(InvoiceSummaryView view, InvoiceListQuery query) {
        InvoiceEntity invoice = view.invoice();
        String customerCode = invoice.getCustomer() == null ? null : invoice.getCustomer().getCustomerCode();
        String customerName = firstNonBlank(invoice.getCustomerName(), invoice.getCustomer() == null ? null : invoice.getCustomer().getCompanyName());
        String contractId = invoice.getContract() == null ? null : invoice.getContract().getId();

        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().toLowerCase(Locale.ROOT);
            boolean matched = containsIgnoreCase(invoice.getInvoiceNumber(), keyword)
                    || containsIgnoreCase(customerCode, keyword)
                    || containsIgnoreCase(customerName, keyword);
            if (!matched) {
                return false;
            }
        }
        if (StringUtils.hasText(query.getInvoiceNumber()) && !containsIgnoreCase(invoice.getInvoiceNumber(), query.getInvoiceNumber().toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (StringUtils.hasText(query.getCustomerId()) && (invoice.getCustomer() == null || !query.getCustomerId().equalsIgnoreCase(invoice.getCustomer().getId()))) {
            return false;
        }
        if (StringUtils.hasText(query.getCustomerName()) && !containsIgnoreCase(customerName, query.getCustomerName().toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (StringUtils.hasText(query.getContractId()) && !query.getContractId().equalsIgnoreCase(contractId)) {
            return false;
        }
        if (StringUtils.hasText(query.getStatus()) && !query.getStatus().equalsIgnoreCase(view.paymentSummary().status())) {
            return false;
        }
        if (query.getIssueFrom() != null && (invoice.getIssueDate() == null || invoice.getIssueDate().isBefore(query.getIssueFrom()))) {
            return false;
        }
        if (query.getIssueTo() != null && (invoice.getIssueDate() == null || invoice.getIssueDate().isAfter(query.getIssueTo()))) {
            return false;
        }
        if (query.getDueFrom() != null && (invoice.getDueDate() == null || invoice.getDueDate().isBefore(query.getDueFrom()))) {
            return false;
        }
        return query.getDueTo() == null || (invoice.getDueDate() != null && !invoice.getDueDate().isAfter(query.getDueTo()));
    }

    private Comparator<InvoiceSummaryView> buildComparator(InvoiceListQuery query) {
        Comparator<InvoiceSummaryView> comparator = switch (query.getSortBy()) {
            case "invoiceNumber" -> Comparator.comparing(view -> normalizeNullable(view.invoice().getInvoiceNumber()), Comparator.nullsLast(String::compareToIgnoreCase));
            case "customerName" -> Comparator.comparing(
                    view -> normalizeNullable(firstNonBlank(view.invoice().getCustomerName(), view.invoice().getCustomer() == null ? null : view.invoice().getCustomer().getCompanyName())),
                    Comparator.nullsLast(String::compareToIgnoreCase)
            );
            case "dueDate" -> Comparator.comparing(view -> view.invoice().getDueDate(), Comparator.nullsLast(LocalDate::compareTo));
            case "grandTotal" -> Comparator.comparing(view -> grandTotal(view.invoice().getTotalAmount(), view.invoice().getVatAmount()));
            case "outstandingAmount" -> Comparator.comparing(view -> view.paymentSummary().outstandingAmount());
            case "status" -> Comparator.comparing(view -> view.paymentSummary().status(), String::compareToIgnoreCase);
            default -> Comparator.comparing(view -> view.invoice().getIssueDate(), Comparator.nullsLast(LocalDate::compareTo));
        };
        return "desc".equalsIgnoreCase(query.getSortDir()) ? comparator.reversed() : comparator;
    }

    private InvoiceAmounts computeInvoiceAmounts(CustomerProfileEntity customer, List<ResolvedInvoiceItem> items, BigDecimal adjustmentAmount) {
        BigDecimal subtotal = items.stream().map(ResolvedInvoiceItem::totalPrice).reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal normalizedAdjustment = normalizeMoney(adjustmentAmount);
        BigDecimal totalAmount = subtotal.add(normalizedAdjustment).setScale(2, RoundingMode.HALF_UP);
        if (totalAmount.compareTo(ZERO) <= 0) {
            throw RequestValidationException.singleError("adjustmentAmount", "Invoice total amount must be greater than 0");
        }
        BigDecimal vatRate = StringUtils.hasText(customer == null ? null : customer.getTaxCode()) ? VAT_WITH_TAX_CODE : ZERO;
        BigDecimal vatAmount = totalAmount.multiply(vatRate).divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);
        return new InvoiceAmounts(normalizedAdjustment, totalAmount, vatRate, vatAmount, totalAmount.add(vatAmount).setScale(2, RoundingMode.HALF_UP));
    }

    private InvoiceEntity maybeSendInvoiceNotification(
            InvoiceEntity invoice,
            AuthenticatedUser currentUser,
            String templateName,
            String subject,
            String auditAction
    ) {
        CustomerProfileEntity customer = invoice.getCustomer();
        if (customer == null || !StringUtils.hasText(customer.getEmail())) {
            return invoice;
        }

        InvoicePaymentSummary paymentSummary = buildPaymentSummaries(List.of(invoice)).get(invoice.getId());
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("recipientName", firstNonBlank(customer.getContactPerson(), customer.getCompanyName()));
        variables.put("customerName", firstNonBlank(invoice.getCustomerName(), customer.getCompanyName()));
        variables.put("invoiceNumber", invoice.getInvoiceNumber());
        variables.put("contractNumber", invoice.getContract() == null ? null : invoice.getContract().getContractNumber());
        variables.put("issueDate", invoice.getIssueDate());
        variables.put("dueDate", invoice.getDueDate());
        variables.put("grandTotal", grandTotal(invoice.getTotalAmount(), invoice.getVatAmount()));
        variables.put("status", paymentSummary.status());
        variables.put("billingAddress", invoice.getBillingAddress());
        variables.put("documentUrl", invoice.getDocumentUrl());
        variables.put("cancellationReason", invoice.getCancellationReason());
        variables.put("note", invoice.getNote());
        variables.put("items", invoice.getItems() == null ? List.of() : invoice.getItems().stream().map(item -> {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("description", firstNonBlank(item.getDescription(), item.getProduct() == null ? null : item.getProduct().getProductName(), "Invoice item"));
            payload.put("unit", firstNonBlank(item.getUnit(), item.getProduct() == null ? null : item.getProduct().getUnit(), "ITEM"));
            payload.put("quantity", normalizeMoney(item.getQuantity()));
            payload.put("unitPrice", normalizeMoney(item.getUnitPrice()));
            payload.put("totalPrice", normalizeMoney(item.getTotalPrice()));
            return payload;
        }).toList());

        emailService.sendHtmlEmail(customer.getEmail(), subject, templateName, variables).join();
        invoice.setNotificationSentAt(LocalDateTime.now(APP_ZONE));
        InvoiceEntity saved = invoiceRepository.save(invoice);
        logAudit(auditAction, "INVOICE", saved.getId(), null, auditPayload(saved), currentUser.userId());
        return saved;
    }

    private String generateInvoiceNumber(LocalDate issueDate) {
        LocalDate effectiveDate = issueDate == null ? LocalDate.now(APP_ZONE) : issueDate;
        LocalDate startOfYear = LocalDate.of(effectiveDate.getYear(), 1, 1);
        LocalDate endOfYear = LocalDate.of(effectiveDate.getYear(), 12, 31);
        long sequence = invoiceRepository.countByIssueDateBetween(startOfYear, endOfYear) + 1;
        return "INV-" + effectiveDate.getYear() + "-" + String.format(Locale.ROOT, "%04d", sequence);
    }

    private String deriveDisplayStatus(String storedStatus, BigDecimal paidAmount, BigDecimal grandTotal) {
        String normalized = normalizeStoredStatus(storedStatus);
        if ("DRAFT".equals(normalized) || "CANCELLED".equals(normalized) || "VOID".equals(normalized) || "SETTLED".equals(normalized)) {
            return normalized;
        }
        if (paidAmount.compareTo(grandTotal) >= 0 && grandTotal.compareTo(ZERO) > 0) {
            return "PAID";
        }
        if (paidAmount.compareTo(ZERO) > 0) {
            return "PARTIALLY_PAID";
        }
        return normalized;
    }

    private String resolveUpdateStatus(String currentStatus, String requestedStatus) {
        if (!StringUtils.hasText(requestedStatus)) {
            return currentStatus;
        }
        String normalized = normalizeUpper(requestedStatus);
        if ("OPEN".equals(normalized)) {
            normalized = "ISSUED";
        }
        if (!ALLOWED_CREATE_STATUSES.contains(normalized)) {
            throw RequestValidationException.singleError("status", "status must be DRAFT or ISSUED");
        }
        if ("ISSUED".equals(currentStatus) && "DRAFT".equals(normalized)) {
            throw RequestValidationException.singleError("status", "Issued invoice cannot be reverted to draft");
        }
        return normalized;
    }

    private void normalizeAndValidateCreateRequest(InvoiceCreateRequest request) {
        request.setContractId(normalizeRequired(request.getContractId(), "contractId", "contractId is required"));
        request.setIssueDate(firstNonNull(request.getIssueDate(), LocalDate.now(APP_ZONE)));
        if (request.getDueDate() == null) {
            throw RequestValidationException.singleError("dueDate", "dueDate is required");
        }
        if (request.getDueDate().isBefore(request.getIssueDate())) {
            throw RequestValidationException.singleError("dueDate", "dueDate must be on or after issueDate");
        }
        request.setAdjustmentAmount(normalizeMoney(request.getAdjustmentAmount()));
        request.setBillingAddress(normalizeNullable(request.getBillingAddress()));
        request.setPaymentTerms(normalizeNullable(request.getPaymentTerms()));
        request.setNote(normalizeNullable(request.getNote()));
        String status = StringUtils.hasText(request.getStatus()) ? normalizeUpper(request.getStatus()) : "ISSUED";
        if ("OPEN".equals(status)) {
            status = "ISSUED";
        }
        if (!ALLOWED_CREATE_STATUSES.contains(status)) {
            throw RequestValidationException.singleError("status", "status must be DRAFT or ISSUED");
        }
        request.setStatus(status);
        if (request.getItems() != null && request.getItems().isEmpty()) {
            request.setItems(null);
        }
    }

    private void normalizeAndValidateUpdateRequest(InvoiceUpdateRequest request) {
        if (request.getIssueDate() != null && request.getDueDate() != null && request.getDueDate().isBefore(request.getIssueDate())) {
            throw RequestValidationException.singleError("dueDate", "dueDate must be on or after issueDate");
        }
        if (request.getAdjustmentAmount() != null) {
            request.setAdjustmentAmount(normalizeMoney(request.getAdjustmentAmount()));
        }
        if (request.getBillingAddress() != null) {
            request.setBillingAddress(normalizeNullable(request.getBillingAddress()));
        }
        if (request.getPaymentTerms() != null) {
            request.setPaymentTerms(normalizeNullable(request.getPaymentTerms()));
        }
        if (request.getNote() != null) {
            request.setNote(normalizeNullable(request.getNote()));
        }
        if (request.getStatus() != null) {
            request.setStatus(normalizeUpper(request.getStatus()));
        }
        if (request.getDocumentUrl() != null) {
            request.setDocumentUrl(normalizeNullable(request.getDocumentUrl()));
        }
    }

    private void normalizeAndValidateListQuery(InvoiceListQuery query) {
        query.setKeyword(normalizeNullable(query.getKeyword()));
        query.setInvoiceNumber(normalizeNullable(query.getInvoiceNumber()));
        query.setCustomerId(normalizeNullable(query.getCustomerId()));
        query.setCustomerName(normalizeNullable(query.getCustomerName()));
        query.setContractId(normalizeNullable(query.getContractId()));
        query.setStatus(StringUtils.hasText(query.getStatus()) ? normalizeUpper(query.getStatus()) : null);
        query.setPage(query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage());
        query.setPageSize(query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize());
        query.setSortBy(StringUtils.hasText(query.getSortBy()) ? query.getSortBy().trim() : "issueDate");
        query.setSortDir(StringUtils.hasText(query.getSortDir()) ? query.getSortDir().trim().toLowerCase(Locale.ROOT) : "desc");
        if (query.getIssueFrom() != null && query.getIssueTo() != null && query.getIssueFrom().isAfter(query.getIssueTo())) {
            throw RequestValidationException.singleError("issueFrom", "issueFrom must be on or before issueTo");
        }
        if (query.getDueFrom() != null && query.getDueTo() != null && query.getDueFrom().isAfter(query.getDueTo())) {
            throw RequestValidationException.singleError("dueFrom", "dueFrom must be on or before dueTo");
        }
        if (StringUtils.hasText(query.getStatus()) && !ALLOWED_LIST_STATUSES.contains(query.getStatus())) {
            throw RequestValidationException.singleError("status", "status must be one of DRAFT, ISSUED, PARTIALLY_PAID, PAID, CANCELLED, VOID");
        }
        if (!ALLOWED_SORT_FIELDS.contains(query.getSortBy())) {
            throw RequestValidationException.singleError("sortBy", "sortBy must be one of invoiceNumber, customerName, issueDate, dueDate, grandTotal, outstandingAmount, status");
        }
        if (!ALLOWED_SORT_DIRECTIONS.contains(query.getSortDir())) {
            throw RequestValidationException.singleError("sortDir", "sortDir must be asc or desc");
        }
    }

    private AuthenticatedUser requireInvoiceManager() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        RoleName role = roleOf(currentUser);
        if (role != RoleName.ACCOUNTANT && role != RoleName.OWNER) {
            throw new ForbiddenOperationException("Only accountant users can manage invoices");
        }
        return currentUser;
    }

    private AuthenticatedUser requireInvoiceViewer() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        RoleName role = roleOf(currentUser);
        if (role != RoleName.ACCOUNTANT && role != RoleName.OWNER && role != RoleName.CUSTOMER) {
            throw new ForbiddenOperationException("You do not have permission to view invoices");
        }
        return currentUser;
    }

    private AuthenticatedUser requireFinanceManager() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (roleOf(currentUser) != RoleName.OWNER) {
            throw new ForbiddenOperationException("Invoice cancellation requires finance manager authority");
        }
        return currentUser;
    }

    private CustomerProfileEntity loadCustomerForCurrentUser(AuthenticatedUser currentUser) {
        return customerProfileRepository.findByUser_Id(currentUser.userId()).orElseThrow(CustomerProfileNotFoundException::new);
    }

    private RoleName roleOf(AuthenticatedUser currentUser) {
        return RoleName.from(currentUser.role());
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

    private Map<String, Object> auditPayload(InvoiceEntity invoice) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("invoiceId", invoice.getId());
        payload.put("invoiceNumber", invoice.getInvoiceNumber());
        payload.put("status", normalizeStoredStatus(invoice.getStatus()));
        payload.put("issueDate", invoice.getIssueDate());
        payload.put("dueDate", invoice.getDueDate());
        payload.put("totalAmount", normalizeMoney(invoice.getTotalAmount()));
        payload.put("vatAmount", normalizeMoney(invoice.getVatAmount()));
        payload.put("customerId", invoice.getCustomer() == null ? null : invoice.getCustomer().getId());
        payload.put("contractId", invoice.getContract() == null ? null : invoice.getContract().getId());
        return payload;
    }

    private Map<String, Object> auditPayload(InvoiceResponse invoice) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("invoiceId", invoice.id());
        payload.put("invoiceNumber", invoice.invoiceNumber());
        payload.put("status", invoice.status());
        payload.put("issueDate", invoice.issueDate());
        payload.put("dueDate", invoice.dueDate());
        payload.put("totalAmount", invoice.totalAmount());
        payload.put("vatAmount", invoice.vatAmount());
        payload.put("customerId", invoice.customerId());
        payload.put("contractId", invoice.contractId());
        return payload;
    }

    private InvoicePaymentSummary emptyPaymentSummary(String status) {
        return new InvoicePaymentSummary(ZERO, ZERO, status);
    }

    private BigDecimal grandTotal(BigDecimal totalAmount, BigDecimal vatAmount) {
        return normalizeMoney(totalAmount).add(normalizeMoney(vatAmount)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizePositiveMoney(BigDecimal value, String field, String message) {
        BigDecimal normalized = normalizeMoney(value);
        if (normalized.compareTo(ZERO) <= 0) {
            throw RequestValidationException.singleError(field, message);
        }
        return normalized;
    }

    private String buildReceiptNumber(String paymentId) {
        String suffix = paymentId == null ? "UNKNOWN" : paymentId.replace("-", "").toUpperCase(Locale.ROOT);
        return "RCPT-" + suffix.substring(0, Math.min(8, suffix.length()));
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

    private String normalizeStoredStatus(String value) {
        String normalized = normalizeUpper(value);
        if (!StringUtils.hasText(normalized) || "OPEN".equals(normalized)) {
            return "ISSUED";
        }
        return normalized;
    }

    private boolean containsIgnoreCase(String value, String lowerCaseQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerCaseQuery);
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private record ResolvedInvoiceItem(
            ProductEntity product,
            String description,
            String unit,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {
    }

    private record InvoiceAmounts(
            BigDecimal adjustmentAmount,
            BigDecimal totalAmount,
            BigDecimal vatRate,
            BigDecimal vatAmount,
            BigDecimal grandTotal
    ) {
    }

    private record InvoicePaymentSummary(
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,
            String status
    ) {
    }

    private record InvoiceSummaryView(
            InvoiceEntity invoice,
            InvoicePaymentSummary paymentSummary
    ) {
    }
}
