package com.g90.backend.modules.saleorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.CustomerProfileNotFoundException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.exception.SaleOrderNotFoundException;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.contract.dto.ContractApprovalResponseData;
import com.g90.backend.modules.contract.dto.ContractCancelRequest;
import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.contract.entity.ContractItemEntity;
import com.g90.backend.modules.contract.entity.ContractStatus;
import com.g90.backend.modules.contract.entity.ContractStatusHistoryEntity;
import com.g90.backend.modules.contract.entity.ContractTrackingEventEntity;
import com.g90.backend.modules.contract.entity.ContractTrackingEventType;
import com.g90.backend.modules.contract.integration.ContractInventoryGateway;
import com.g90.backend.modules.contract.repository.ContractRepository;
import com.g90.backend.modules.contract.repository.ContractStatusHistoryRepository;
import com.g90.backend.modules.contract.repository.ContractTrackingEventRepository;
import com.g90.backend.modules.contract.service.ContractService;
import com.g90.backend.modules.debt.repository.PaymentAllocationRepository;
import com.g90.backend.modules.inventory.entity.InventoryTransactionEntity;
import com.g90.backend.modules.inventory.repository.InventoryTransactionRepository;
import com.g90.backend.modules.payment.dto.InvoiceCreateRequest;
import com.g90.backend.modules.payment.dto.InvoiceResponse;
import com.g90.backend.modules.payment.entity.InvoiceEntity;
import com.g90.backend.modules.payment.repository.InvoiceRepository;
import com.g90.backend.modules.payment.service.InvoiceService;
import com.g90.backend.modules.product.dto.PaginationResponse;
import com.g90.backend.modules.project.entity.ProjectManagementEntity;
import com.g90.backend.modules.project.repository.ProjectManagementRepository;
import com.g90.backend.modules.saleorder.dto.SaleOrderActionRequest;
import com.g90.backend.modules.saleorder.dto.SaleOrderActionResponseData;
import com.g90.backend.modules.saleorder.dto.SaleOrderCancelRequest;
import com.g90.backend.modules.saleorder.dto.SaleOrderDetailResponseData;
import com.g90.backend.modules.saleorder.dto.SaleOrderInvoiceCreateRequest;
import com.g90.backend.modules.saleorder.dto.SaleOrderListQuery;
import com.g90.backend.modules.saleorder.dto.SaleOrderListResponseData;
import com.g90.backend.modules.saleorder.dto.SaleOrderStatusUpdateRequest;
import com.g90.backend.modules.saleorder.dto.SaleOrderTimelineResponseData;
import com.g90.backend.modules.saleorder.repository.SaleOrderSpecifications;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SaleOrderServiceImpl implements SaleOrderService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Set<String> VIEW_ROLES = Set.of(
            RoleName.CUSTOMER.name(),
            RoleName.ACCOUNTANT.name(),
            RoleName.OWNER.name(),
            RoleName.WAREHOUSE.name()
    );
    private static final Set<String> INTERNAL_ROLES = Set.of(
            RoleName.ACCOUNTANT.name(),
            RoleName.OWNER.name(),
            RoleName.WAREHOUSE.name()
    );
    private static final Set<String> ACTIONABLE_STATUSES = Set.of(
            ContractStatus.SUBMITTED.name(),
            ContractStatus.PROCESSING.name(),
            ContractStatus.RESERVED.name(),
            ContractStatus.PICKED.name(),
            ContractStatus.IN_TRANSIT.name(),
            ContractStatus.DELIVERED.name(),
            ContractStatus.COMPLETED.name(),
            ContractStatus.CANCELLED.name()
    );
    private static final Set<String> MANUAL_STATUSES = Set.of(
            ContractStatus.PROCESSING.name(),
            ContractStatus.RESERVED.name(),
            ContractStatus.PICKED.name(),
            ContractStatus.IN_TRANSIT.name(),
            ContractStatus.DELIVERED.name(),
            ContractStatus.COMPLETED.name()
    );
    private static final Set<String> SALE_ORDER_SORT_FIELDS = Set.of(
            "saleOrderNumber",
            "contractNumber",
            "orderDate",
            "expectedDeliveryDate",
            "actualDeliveryDate",
            "totalAmount",
            "status"
    );
    private static final List<String> TIMELINE_STATUSES = List.of(
            ContractStatus.SUBMITTED.name(),
            ContractStatus.PROCESSING.name(),
            ContractStatus.RESERVED.name(),
            ContractStatus.PICKED.name(),
            ContractStatus.IN_TRANSIT.name(),
            ContractStatus.DELIVERED.name(),
            ContractStatus.COMPLETED.name()
    );
    private static final Set<String> ISSUE_ALLOWED_STATUSES = Set.of(
            ContractStatus.PICKED.name()
    );

    private final ContractRepository contractRepository;
    private final ContractStatusHistoryRepository contractStatusHistoryRepository;
    private final ContractTrackingEventRepository contractTrackingEventRepository;
    private final ProjectManagementRepository projectManagementRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ContractInventoryGateway contractInventoryGateway;
    private final ContractService contractService;
    private final InvoiceService invoiceService;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public SaleOrderListResponseData getSaleOrders(SaleOrderListQuery query) {
        AuthenticatedUser currentUser = requireViewer();
        normalizeListQuery(query);

        String customerScope = roleOf(currentUser) == RoleName.CUSTOMER
                ? loadCustomerForCurrentUser(currentUser).getId()
                : null;

        Page<ContractEntity> page = contractRepository.findAll(
                SaleOrderSpecifications.byQuery(query, customerScope),
                PageRequest.of(query.getPage() - 1, query.getPageSize(), buildSort(query))
        );

        Map<String, ProjectManagementEntity> projectByContractId = loadProjects(page.getContent());
        return new SaleOrderListResponseData(
                page.getContent().stream()
                        .map(contract -> toListItem(contract, projectByContractId.get(contract.getId())))
                        .toList(),
                PaginationResponse.builder()
                        .page(query.getPage())
                        .pageSize(query.getPageSize())
                        .totalItems(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build(),
                new SaleOrderListResponseData.Filters(
                        query.getKeyword(),
                        query.getSaleOrderNumber(),
                        query.getContractNumber(),
                        query.getCustomerId(),
                        query.getProjectId(),
                        query.getStatus(),
                        query.getOrderFrom(),
                        query.getOrderTo(),
                        query.getDeliveryFrom(),
                        query.getDeliveryTo()
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SaleOrderDetailResponseData getSaleOrder(String saleOrderId) {
        return buildDetailResponse(loadAccessibleSaleOrder(saleOrderId, requireViewer()));
    }

    @Override
    @Transactional(readOnly = true)
    public SaleOrderTimelineResponseData getTimeline(String saleOrderId) {
        ContractEntity saleOrder = loadAccessibleSaleOrder(saleOrderId, requireViewer());
        ProjectManagementEntity project = projectManagementRepository.findByLinkedContract_Id(saleOrder.getId()).orElse(null);
        List<ContractStatusHistoryEntity> history = contractStatusHistoryRepository.findByContract_IdOrderByChangedAtAsc(saleOrder.getId());
        List<ContractTrackingEventEntity> events = contractTrackingEventRepository.findByContract_IdOrderByActualAtAscCreatedAtAsc(saleOrder.getId());
        return new SaleOrderTimelineResponseData(
                saleOrder.getId(),
                saleOrderNumberOf(saleOrder),
                saleOrder.getContractNumber(),
                saleOrder.getStatus(),
                firstNonNull(saleOrder.getLastTrackingRefreshAt(), saleOrder.getUpdatedAt(), saleOrder.getCreatedAt()),
                buildMilestones(saleOrder, history, project),
                events.stream().map(this::toTimelineEvent).toList()
        );
    }

    @Override
    @Transactional
    public SaleOrderActionResponseData updateStatus(String saleOrderId, SaleOrderStatusUpdateRequest request) {
        AuthenticatedUser currentUser = requireInternalOperator();
        request.setStatus(normalizeRequired(request.getStatus(), "status", "status is required"));
        request.setNote(normalizeNullable(request.getNote()));
        request.setTrackingNumber(normalizeNullable(request.getTrackingNumber()));

        ContractStatus targetStatus = parseTargetStatus(request.getStatus());
        if (!MANUAL_STATUSES.contains(targetStatus.name())) {
            throw RequestValidationException.singleError("status", "status must be one of PROCESSING, RESERVED, PICKED, IN_TRANSIT, DELIVERED, COMPLETED");
        }
        return changeStatus(saleOrderId, targetStatus, request.getNote(), request.getActualDeliveryDate(), request.getTrackingNumber(), currentUser);
    }

    @Override
    @Transactional
    public SaleOrderActionResponseData reserve(String saleOrderId, SaleOrderActionRequest request) {
        SaleOrderActionRequest normalized = normalizeRequest(request);
        return changeStatus(saleOrderId, ContractStatus.RESERVED, normalized.getNote(), normalized.getActualDeliveryDate(), normalized.getTrackingNumber(), requireInternalOperator());
    }

    @Override
    @Transactional
    public SaleOrderActionResponseData pick(String saleOrderId, SaleOrderActionRequest request) {
        SaleOrderActionRequest normalized = normalizeRequest(request);
        return changeStatus(saleOrderId, ContractStatus.PICKED, normalized.getNote(), normalized.getActualDeliveryDate(), normalized.getTrackingNumber(), requireInternalOperator());
    }

    @Override
    @Transactional
    public SaleOrderActionResponseData dispatch(String saleOrderId, SaleOrderActionRequest request) {
        SaleOrderActionRequest normalized = normalizeRequest(request);
        return changeStatus(saleOrderId, ContractStatus.IN_TRANSIT, normalized.getNote(), normalized.getActualDeliveryDate(), normalized.getTrackingNumber(), requireInternalOperator());
    }

    @Override
    @Transactional
    public SaleOrderActionResponseData deliver(String saleOrderId, SaleOrderActionRequest request) {
        SaleOrderActionRequest normalized = normalizeRequest(request);
        return changeStatus(saleOrderId, ContractStatus.DELIVERED, normalized.getNote(), normalized.getActualDeliveryDate(), normalized.getTrackingNumber(), requireInternalOperator());
    }

    @Override
    @Transactional
    public SaleOrderActionResponseData complete(String saleOrderId, SaleOrderActionRequest request) {
        SaleOrderActionRequest normalized = normalizeRequest(request);
        return changeStatus(saleOrderId, ContractStatus.COMPLETED, normalized.getNote(), normalized.getActualDeliveryDate(), normalized.getTrackingNumber(), requireInternalOperator());
    }

    @Override
    @Transactional
    public SaleOrderActionResponseData cancel(String saleOrderId, SaleOrderCancelRequest request) {
        AuthenticatedUser currentUser = requireInternalOperator();
        ContractEntity saleOrder = loadAccessibleSaleOrder(saleOrderId, currentUser);
        String previousStatus = saleOrder.getStatus();

        ContractCancelRequest cancelRequest = new ContractCancelRequest();
        cancelRequest.setCancellationReason(request.getCancellationReason());
        cancelRequest.setCancellationNote(normalizeNullable(request.getComment()));

        ContractApprovalResponseData response = contractService.cancelContract(saleOrderId, cancelRequest);
        ContractEntity updated = contractRepository.findDetailedById(saleOrderId).orElse(saleOrder);
        return new SaleOrderActionResponseData(
                updated.getId(),
                saleOrderNumberOf(updated),
                updated.getContractNumber(),
                previousStatus,
                response.contractStatus(),
                response.approvalStatus(),
                response.decision(),
                response.decidedBy(),
                response.decidedAt(),
                response.comment(),
                null
        );
    }

    @Override
    @Transactional
    public InvoiceResponse createInvoice(String saleOrderId, SaleOrderInvoiceCreateRequest request) {
        requireAccountingOperator();
        ContractEntity saleOrder = loadAccessibleSaleOrder(saleOrderId, currentUserProvider.getCurrentUser());

        InvoiceCreateRequest invoiceRequest = new InvoiceCreateRequest();
        invoiceRequest.setContractId(saleOrder.getId());
        invoiceRequest.setIssueDate(request.getIssueDate());
        invoiceRequest.setDueDate(request.getDueDate());
        invoiceRequest.setAdjustmentAmount(request.getAdjustmentAmount());
        invoiceRequest.setBillingAddress(request.getBillingAddress());
        invoiceRequest.setPaymentTerms(request.getPaymentTerms());
        invoiceRequest.setNote(request.getNote());
        invoiceRequest.setStatus(request.getStatus());
        invoiceRequest.setItems(request.getItems());
        return invoiceService.createInvoice(invoiceRequest);
    }

    @Override
    @Transactional
    public void registerInventoryIssue(String saleOrderId, String productId, BigDecimal quantity, String note, String userId) {
        if (!StringUtils.hasText(saleOrderId)) {
            return;
        }
        if (!StringUtils.hasText(productId)) {
            throw RequestValidationException.singleError("productId", "productId is required");
        }
        BigDecimal normalizedQuantity = normalizeQuantity(quantity);
        if (normalizedQuantity.compareTo(ZERO) <= 0) {
            throw RequestValidationException.singleError("quantity", "quantity must be greater than 0");
        }

        ContractEntity saleOrder = contractRepository.findDetailedById(saleOrderId.trim()).orElseThrow(SaleOrderNotFoundException::new);
        ensureSaleOrder(saleOrder);
        String currentStatus = normalizeUpper(saleOrder.getStatus());
        if (!ISSUE_ALLOWED_STATUSES.contains(currentStatus)) {
            throw RequestValidationException.singleError("relatedOrderId", "Inventory issue is not allowed for the current sale order status");
        }

        ContractItemEntity item = saleOrder.getItems().stream()
                .filter(contractItem -> contractItem.getProduct() != null && productId.trim().equals(contractItem.getProduct().getId()))
                .findFirst()
                .orElseThrow(() -> RequestValidationException.singleError("productId", "Product does not belong to the selected sale order"));

        BigDecimal orderedQuantity = normalizeQuantity(item.getQuantity());
        BigDecimal issuedQuantity = normalizeQuantity(item.getIssuedQuantity());
        BigDecimal remainingQuantity = orderedQuantity.subtract(issuedQuantity).setScale(2, RoundingMode.HALF_UP);
        if (remainingQuantity.compareTo(normalizedQuantity) < 0) {
            throw RequestValidationException.singleError("quantity", "Issued quantity exceeds ordered quantity");
        }

        item.setIssuedQuantity(issuedQuantity.add(normalizedQuantity).setScale(2, RoundingMode.HALF_UP));
        item.setReservedQuantity(normalizeQuantity(item.getReservedQuantity()).max(item.getIssuedQuantity()));
        saleOrder.setUpdatedBy(userId);
        saleOrder.setLastTrackingRefreshAt(LocalDateTime.now(APP_ZONE));

        contractRepository.save(saleOrder);
        logAudit(
                "REGISTER_SALE_ORDER_ISSUE",
                saleOrder.getId(),
                Map.of("status", saleOrder.getStatus(), "productId", productId.trim(), "issuedQuantity", issuedQuantity),
                Map.of("status", saleOrder.getStatus(), "productId", productId.trim(), "issuedQuantity", item.getIssuedQuantity()),
                userId
        );
    }

    private SaleOrderDetailResponseData buildDetailResponse(ContractEntity saleOrder) {
        ProjectManagementEntity project = projectManagementRepository.findByLinkedContract_Id(saleOrder.getId()).orElse(null);
        List<ContractTrackingEventEntity> events = contractTrackingEventRepository.findByContract_IdOrderByActualAtAscCreatedAtAsc(saleOrder.getId());
        List<InventoryTransactionEntity> inventoryIssues = inventoryTransactionRepository.findByRelatedOrderIdOrderByTransactionDateDescCreatedAtDesc(saleOrder.getId());
        List<InvoiceEntity> invoices = invoiceRepository.findByContractIdWithCustomerAndContract(saleOrder.getId());
        Map<String, InvoiceFinancialSummary> invoiceSummaries = buildInvoiceSummaries(invoices);
        List<SaleOrderDetailResponseData.ItemData> items = saleOrder.getItems().stream().map(this::toItemData).toList();

        return new SaleOrderDetailResponseData(
                new SaleOrderDetailResponseData.HeaderData(
                        saleOrder.getId(),
                        saleOrderNumberOf(saleOrder),
                        saleOrder.getId(),
                        saleOrder.getContractNumber(),
                        saleOrder.getStatus(),
                        saleOrder.getSubmittedAt() == null ? null : saleOrder.getSubmittedAt().toLocalDate(),
                        saleOrder.getSubmittedAt(),
                        saleOrder.getApprovedAt(),
                        saleOrder.getExpectedDeliveryDate(),
                        saleOrder.getActualDeliveryDate(),
                        normalizeMoney(saleOrder.getTotalAmount()),
                        normalizeNullable(saleOrder.getNote())
                ),
                new SaleOrderDetailResponseData.CustomerData(
                        saleOrder.getCustomer() == null ? null : saleOrder.getCustomer().getId(),
                        saleOrder.getCustomer() == null ? null : saleOrder.getCustomer().getCustomerCode(),
                        saleOrder.getCustomer() == null ? null : saleOrder.getCustomer().getCompanyName(),
                        saleOrder.getCustomer() == null ? null : saleOrder.getCustomer().getContactPerson(),
                        saleOrder.getCustomer() == null ? null : saleOrder.getCustomer().getPhone(),
                        saleOrder.getCustomer() == null ? null : saleOrder.getCustomer().getEmail(),
                        saleOrder.getCustomer() == null ? null : saleOrder.getCustomer().getAddress()
                ),
                project == null ? null : new SaleOrderDetailResponseData.ProjectData(project.getId(), project.getProjectCode(), project.getName(), project.getStatus()),
                items,
                new SaleOrderDetailResponseData.FulfillmentSummaryData(
                        sum(items, SaleOrderDetailResponseData.ItemData::orderedQuantity),
                        sum(items, SaleOrderDetailResponseData.ItemData::reservedQuantity),
                        sum(items, SaleOrderDetailResponseData.ItemData::issuedQuantity),
                        sum(items, SaleOrderDetailResponseData.ItemData::deliveredQuantity),
                        items.size(),
                        inventoryIssues.size(),
                        invoices.size()
                ),
                events.stream().map(this::toDetailTimelineEvent).toList(),
                inventoryIssues.stream().map(this::toInventoryIssueData).toList(),
                invoices.stream()
                        .map(invoice -> toInvoiceData(invoice, invoiceSummaries.getOrDefault(invoice.getId(), emptyInvoiceSummary(invoice))))
                        .toList()
        );
    }

    private SaleOrderActionResponseData changeStatus(
            String saleOrderId,
            ContractStatus targetStatus,
            String note,
            LocalDate actualDeliveryDate,
            String trackingNumber,
            AuthenticatedUser currentUser
    ) {
        ContractEntity saleOrder = loadAccessibleSaleOrder(saleOrderId, currentUser);
        String previousStatus = saleOrder.getStatus();

        validateTransition(saleOrder, targetStatus);
        applyTransition(saleOrder, targetStatus, actualDeliveryDate, currentUser.userId());

        ContractEntity saved = contractRepository.save(saleOrder);
        recordStatusHistory(saved, previousStatus, saved.getStatus(), normalizeNullable(note), currentUser.userId());
        recordTrackingEvent(saved, eventTypeFor(targetStatus), saved.getStatus(), titleFor(targetStatus), normalizeNullable(note), null, normalizeNullable(trackingNumber), currentUser.userId());
        logAudit("UPDATE_SALE_ORDER_STATUS", saved.getId(), previousStatus, saved.getStatus(), currentUser.userId());

        return new SaleOrderActionResponseData(
                saved.getId(),
                saleOrderNumberOf(saved),
                saved.getContractNumber(),
                previousStatus,
                saved.getStatus(),
                saved.getApprovalStatus(),
                targetStatus.name(),
                currentUser.userId(),
                saved.getLastStatusChangeAt(),
                normalizeNullable(note),
                normalizeNullable(trackingNumber)
        );
    }

    private void applyTransition(ContractEntity saleOrder, ContractStatus targetStatus, LocalDate actualDeliveryDate, String userId) {
        if (targetStatus == ContractStatus.RESERVED) {
            assertInventoryAvailable(saleOrder);
            saleOrder.getItems().forEach(item -> item.setReservedQuantity(normalizeQuantity(item.getQuantity())));
        }
        if (targetStatus == ContractStatus.IN_TRANSIT || targetStatus == ContractStatus.DELIVERED) {
            ensureAllIssued(saleOrder);
        }
        if (targetStatus == ContractStatus.DELIVERED) {
            saleOrder.getItems().forEach(item -> item.setDeliveredQuantity(normalizeQuantity(item.getQuantity())));
            saleOrder.setActualDeliveryDate(actualDeliveryDate == null ? LocalDate.now(APP_ZONE) : actualDeliveryDate);
        }
        if (targetStatus == ContractStatus.COMPLETED) {
            ensureAllDelivered(saleOrder);
        }

        saleOrder.setStatus(targetStatus.name());
        saleOrder.setUpdatedBy(userId);
        saleOrder.setLastStatusChangeAt(LocalDateTime.now(APP_ZONE));
        saleOrder.setLastTrackingRefreshAt(LocalDateTime.now(APP_ZONE));
    }

    private void validateTransition(ContractEntity saleOrder, ContractStatus targetStatus) {
        ContractStatus currentStatus = parseTargetStatus(saleOrder.getStatus());
        if (currentStatus == targetStatus) {
            throw RequestValidationException.singleError("status", "Sale order is already in the requested status");
        }
        if (currentStatus == ContractStatus.CANCELLED || currentStatus == ContractStatus.COMPLETED) {
            throw RequestValidationException.singleError("status", "Terminal sale orders cannot change status");
        }
        if (!ACTIONABLE_STATUSES.contains(currentStatus.name())) {
            throw RequestValidationException.singleError("status", "Sale order is not yet executable");
        }

        boolean allowed = switch (targetStatus) {
            case PROCESSING -> currentStatus == ContractStatus.SUBMITTED;
            case RESERVED -> currentStatus == ContractStatus.SUBMITTED || currentStatus == ContractStatus.PROCESSING;
            case PICKED -> currentStatus == ContractStatus.RESERVED;
            case IN_TRANSIT -> currentStatus == ContractStatus.PICKED;
            case DELIVERED -> currentStatus == ContractStatus.IN_TRANSIT;
            case COMPLETED -> currentStatus == ContractStatus.DELIVERED;
            default -> false;
        };
        if (!allowed) {
            throw RequestValidationException.singleError("status", "Invalid sale order status transition");
        }
    }

    private void ensureAllIssued(ContractEntity saleOrder) {
        boolean incomplete = saleOrder.getItems().stream().anyMatch(item ->
                normalizeQuantity(item.getIssuedQuantity()).compareTo(normalizeQuantity(item.getQuantity())) < 0
        );
        if (incomplete) {
            throw RequestValidationException.singleError("status", "All sale order items must be fully issued before continuing");
        }
    }

    private void ensureAllDelivered(ContractEntity saleOrder) {
        boolean incomplete = saleOrder.getItems().stream().anyMatch(item ->
                normalizeQuantity(item.getDeliveredQuantity()).compareTo(normalizeQuantity(item.getQuantity())) < 0
        );
        if (incomplete) {
            throw RequestValidationException.singleError("status", "All sale order items must be fully delivered before completion");
        }
    }

    private void assertInventoryAvailable(ContractEntity saleOrder) {
        var requests = saleOrder.getItems().stream()
                .map(item -> new ContractInventoryGateway.RequestedInventory(item.getProduct().getId(), item.getQuantity()))
                .toList();
        Map<String, ContractInventoryGateway.InventoryAvailability> availability = contractInventoryGateway.checkAvailability(requests);
        for (ContractItemEntity item : saleOrder.getItems()) {
            ContractInventoryGateway.InventoryAvailability itemAvailability = availability.get(item.getProduct().getId());
            if (itemAvailability != null && !itemAvailability.sufficient()) {
                throw RequestValidationException.singleError("status", "Insufficient inventory to reserve the sale order");
            }
        }
    }

    private SaleOrderListResponseData.Item toListItem(ContractEntity saleOrder, ProjectManagementEntity project) {
        return new SaleOrderListResponseData.Item(
                saleOrder.getId(),
                saleOrderNumberOf(saleOrder),
                saleOrder.getContractNumber(),
                saleOrder.getCustomer() == null ? null : saleOrder.getCustomer().getId(),
                saleOrder.getCustomer() == null ? null : saleOrder.getCustomer().getCompanyName(),
                project == null ? null : project.getId(),
                project == null ? null : project.getProjectCode(),
                project == null ? null : project.getName(),
                saleOrder.getSubmittedAt() == null ? null : saleOrder.getSubmittedAt().toLocalDate(),
                saleOrder.getExpectedDeliveryDate(),
                saleOrder.getActualDeliveryDate(),
                saleOrder.getStatus(),
                normalizeMoney(saleOrder.getTotalAmount())
        );
    }

    private SaleOrderDetailResponseData.ItemData toItemData(ContractItemEntity item) {
        return new SaleOrderDetailResponseData.ItemData(
                item.getId(),
                item.getProduct() == null ? null : item.getProduct().getId(),
                item.getProduct() == null ? null : item.getProduct().getProductCode(),
                item.getProduct() == null ? null : item.getProduct().getProductName(),
                item.getProduct() == null ? null : item.getProduct().getType(),
                item.getProduct() == null ? null : item.getProduct().getSize(),
                item.getProduct() == null ? null : item.getProduct().getThickness(),
                item.getProduct() == null ? null : item.getProduct().getUnit(),
                normalizeQuantity(item.getQuantity()),
                normalizeQuantity(item.getReservedQuantity()),
                normalizeQuantity(item.getIssuedQuantity()),
                normalizeQuantity(item.getDeliveredQuantity()),
                normalizeMoney(item.getUnitPrice()),
                normalizeMoney(item.getTotalPrice()),
                normalizeNullable(item.getFulfillmentNote())
        );
    }

    private SaleOrderDetailResponseData.TimelineEventData toDetailTimelineEvent(ContractTrackingEventEntity event) {
        return new SaleOrderDetailResponseData.TimelineEventData(
                event.getEventType(),
                event.getEventStatus(),
                event.getTitle(),
                event.getNote(),
                event.getExpectedAt(),
                event.getActualAt(),
                event.getTrackingNumber()
        );
    }

    private SaleOrderTimelineResponseData.Event toTimelineEvent(ContractTrackingEventEntity event) {
        return new SaleOrderTimelineResponseData.Event(
                event.getEventType(),
                event.getEventStatus(),
                event.getTitle(),
                event.getNote(),
                event.getExpectedAt(),
                event.getActualAt(),
                event.getTrackingNumber()
        );
    }

    private SaleOrderDetailResponseData.InventoryIssueData toInventoryIssueData(InventoryTransactionEntity transaction) {
        return new SaleOrderDetailResponseData.InventoryIssueData(
                transaction.getId(),
                transaction.getTransactionCode(),
                transaction.getProduct() == null ? null : transaction.getProduct().getId(),
                transaction.getProduct() == null ? null : transaction.getProduct().getProductCode(),
                transaction.getProduct() == null ? null : transaction.getProduct().getProductName(),
                normalizeQuantity(transaction.getQuantity()),
                transaction.getTransactionDate(),
                normalizeNullable(transaction.getReason()),
                normalizeNullable(transaction.getNote())
        );
    }

    private SaleOrderDetailResponseData.InvoiceData toInvoiceData(InvoiceEntity invoice, InvoiceFinancialSummary summary) {
        return new SaleOrderDetailResponseData.InvoiceData(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                summary.grandTotal(),
                summary.paidAmount(),
                summary.outstandingAmount(),
                summary.status(),
                invoice.getDocumentUrl()
        );
    }

    private List<SaleOrderTimelineResponseData.Milestone> buildMilestones(
            ContractEntity saleOrder,
            List<ContractStatusHistoryEntity> history,
            ProjectManagementEntity project
    ) {
        Map<String, LocalDateTime> reachedAtByStatus = new LinkedHashMap<>();
        if (saleOrder.getSubmittedAt() != null) {
            reachedAtByStatus.put(ContractStatus.SUBMITTED.name(), saleOrder.getSubmittedAt());
        }
        for (ContractStatusHistoryEntity item : history) {
            if (item == null || !StringUtils.hasText(item.getToStatus())) {
                continue;
            }
            String normalized = normalizeUpper(item.getToStatus());
            if (TIMELINE_STATUSES.contains(normalized)) {
                reachedAtByStatus.putIfAbsent(normalized, item.getChangedAt());
            }
        }
        if (saleOrder.getActualDeliveryDate() != null && !reachedAtByStatus.containsKey(ContractStatus.DELIVERED.name())) {
            reachedAtByStatus.put(ContractStatus.DELIVERED.name(), firstNonNull(saleOrder.getUpdatedAt(), saleOrder.getCreatedAt()));
        }
        return TIMELINE_STATUSES.stream()
                .map(status -> new SaleOrderTimelineResponseData.Milestone(
                        status,
                        milestoneTitle(status, project),
                        reachedAtByStatus.containsKey(status),
                        reachedAtByStatus.get(status)
                ))
                .toList();
    }

    private Map<String, ProjectManagementEntity> loadProjects(List<ContractEntity> contracts) {
        Map<String, ProjectManagementEntity> projectByContractId = new LinkedHashMap<>();
        List<String> contractIds = contracts.stream().map(ContractEntity::getId).toList();
        if (contractIds.isEmpty()) {
            return projectByContractId;
        }
        for (ProjectManagementEntity project : projectManagementRepository.findByLinkedContract_IdIn(contractIds)) {
            if (project.getLinkedContract() != null) {
                projectByContractId.putIfAbsent(project.getLinkedContract().getId(), project);
            }
        }
        return projectByContractId;
    }

    private Map<String, InvoiceFinancialSummary> buildInvoiceSummaries(Collection<InvoiceEntity> invoices) {
        Map<String, InvoiceFinancialSummary> summaries = new LinkedHashMap<>();
        if (invoices == null || invoices.isEmpty()) {
            return summaries;
        }
        Map<String, BigDecimal> paidByInvoiceId = new LinkedHashMap<>();
        paymentAllocationRepository.summarizeByInvoiceIds(invoices.stream().map(InvoiceEntity::getId).toList())
                .forEach(view -> paidByInvoiceId.put(view.getInvoiceId(), normalizeMoney(view.getAllocatedAmount())));

        for (InvoiceEntity invoice : invoices) {
            BigDecimal grandTotal = normalizeMoney(invoice.getTotalAmount()).add(normalizeMoney(invoice.getVatAmount())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal paidAmount = paidByInvoiceId.getOrDefault(invoice.getId(), ZERO);
            BigDecimal outstandingAmount = grandTotal.subtract(paidAmount).max(ZERO).setScale(2, RoundingMode.HALF_UP);
            summaries.put(invoice.getId(), new InvoiceFinancialSummary(
                    grandTotal,
                    paidAmount,
                    outstandingAmount,
                    deriveInvoiceStatus(invoice.getStatus(), paidAmount, grandTotal)
            ));
        }
        return summaries;
    }

    private InvoiceFinancialSummary emptyInvoiceSummary(InvoiceEntity invoice) {
        BigDecimal grandTotal = normalizeMoney(invoice.getTotalAmount()).add(normalizeMoney(invoice.getVatAmount())).setScale(2, RoundingMode.HALF_UP);
        return new InvoiceFinancialSummary(grandTotal, ZERO, grandTotal, normalizeInvoiceStatus(invoice.getStatus()));
    }

    private ContractEntity loadAccessibleSaleOrder(String saleOrderId, AuthenticatedUser currentUser) {
        RoleName role = roleOf(currentUser);
        ContractEntity saleOrder;
        if (role == RoleName.CUSTOMER) {
            CustomerProfileEntity customer = loadCustomerForCurrentUser(currentUser);
            saleOrder = contractRepository.findDetailedByIdAndCustomer_Id(saleOrderId, customer.getId()).orElseThrow(SaleOrderNotFoundException::new);
        } else if (VIEW_ROLES.contains(role.name())) {
            saleOrder = contractRepository.findDetailedById(saleOrderId).orElseThrow(SaleOrderNotFoundException::new);
        } else {
            throw new ForbiddenOperationException("You do not have permission to access sale orders");
        }
        ensureSaleOrder(saleOrder);
        return saleOrder;
    }

    private void ensureSaleOrder(ContractEntity contract) {
        if (contract == null || (!StringUtils.hasText(contract.getSaleOrderNumber()) && contract.getSubmittedAt() == null)) {
            throw new SaleOrderNotFoundException();
        }
    }

    private void normalizeListQuery(SaleOrderListQuery query) {
        query.setPage(query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage());
        query.setPageSize(query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize());
        query.setKeyword(normalizeNullable(query.getKeyword()));
        query.setSaleOrderNumber(normalizeNullable(query.getSaleOrderNumber()));
        query.setContractNumber(normalizeNullable(query.getContractNumber()));
        query.setCustomerId(normalizeNullable(query.getCustomerId()));
        query.setProjectId(normalizeNullable(query.getProjectId()));
        query.setStatus(StringUtils.hasText(query.getStatus()) ? normalizeUpper(query.getStatus()) : null);
        query.setSortBy(StringUtils.hasText(query.getSortBy()) ? query.getSortBy().trim() : "orderDate");
        query.setSortDir(StringUtils.hasText(query.getSortDir()) ? query.getSortDir().trim().toLowerCase(Locale.ROOT) : "desc");
        if (query.getOrderFrom() != null && query.getOrderTo() != null && query.getOrderFrom().isAfter(query.getOrderTo())) {
            throw RequestValidationException.singleError("orderFrom", "orderFrom must be on or before orderTo");
        }
        if (query.getDeliveryFrom() != null && query.getDeliveryTo() != null && query.getDeliveryFrom().isAfter(query.getDeliveryTo())) {
            throw RequestValidationException.singleError("deliveryFrom", "deliveryFrom must be on or before deliveryTo");
        }
        if (StringUtils.hasText(query.getStatus()) && !ACTIONABLE_STATUSES.contains(query.getStatus())) {
            throw RequestValidationException.singleError("status", "status is not a valid sale order status");
        }
        if (!SALE_ORDER_SORT_FIELDS.contains(query.getSortBy())) {
            throw RequestValidationException.singleError("sortBy", "sortBy must be one of saleOrderNumber, contractNumber, orderDate, expectedDeliveryDate, actualDeliveryDate, totalAmount, status");
        }
        if (!"asc".equals(query.getSortDir()) && !"desc".equals(query.getSortDir())) {
            throw RequestValidationException.singleError("sortDir", "sortDir must be asc or desc");
        }
    }

    private Sort buildSort(SaleOrderListQuery query) {
        String sortBy = "orderDate".equals(query.getSortBy()) ? "submittedAt" : query.getSortBy();
        Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sortBy);
    }

    private SaleOrderActionRequest normalizeRequest(SaleOrderActionRequest request) {
        SaleOrderActionRequest normalized = request == null ? new SaleOrderActionRequest() : request;
        normalized.setNote(normalizeNullable(normalized.getNote()));
        normalized.setTrackingNumber(normalizeNullable(normalized.getTrackingNumber()));
        return normalized;
    }

    private ContractStatus parseTargetStatus(String status) {
        try {
            return ContractStatus.from(status);
        } catch (IllegalArgumentException exception) {
            throw RequestValidationException.singleError("status", "Invalid sale order status");
        }
    }

    private ContractTrackingEventType eventTypeFor(ContractStatus targetStatus) {
        return switch (targetStatus) {
            case PROCESSING -> ContractTrackingEventType.PROCESSING_STARTED;
            case RESERVED -> ContractTrackingEventType.RESERVED;
            case PICKED -> ContractTrackingEventType.PICKED;
            case IN_TRANSIT -> ContractTrackingEventType.SHIPPED;
            case DELIVERED -> ContractTrackingEventType.DELIVERED;
            case COMPLETED -> ContractTrackingEventType.COMPLETED;
            default -> ContractTrackingEventType.UPDATED;
        };
    }

    private String titleFor(ContractStatus targetStatus) {
        return switch (targetStatus) {
            case PROCESSING -> "Order processing started";
            case RESERVED -> "Inventory reserved";
            case PICKED -> "Order picked";
            case IN_TRANSIT -> "Order dispatched";
            case DELIVERED -> "Order delivered";
            case COMPLETED -> "Order completed";
            default -> "Sale order updated";
        };
    }

    private String milestoneTitle(String status, ProjectManagementEntity project) {
        return switch (status) {
            case "SUBMITTED" -> "Submitted";
            case "PROCESSING" -> "Processing";
            case "RESERVED" -> "Reserved";
            case "PICKED" -> "Picked";
            case "IN_TRANSIT" -> project == null ? "In Transit" : "In Transit to " + project.getName();
            case "DELIVERED" -> "Delivered";
            case "COMPLETED" -> "Completed";
            default -> status;
        };
    }

    private void recordStatusHistory(ContractEntity contract, String fromStatus, String toStatus, String reason, String changedBy) {
        ContractStatusHistoryEntity history = new ContractStatusHistoryEntity();
        history.setContract(contract);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangeReason(normalizeNullable(reason));
        history.setChangedBy(changedBy);
        contractStatusHistoryRepository.save(history);
    }

    private void recordTrackingEvent(
            ContractEntity contract,
            ContractTrackingEventType eventType,
            String eventStatus,
            String title,
            String note,
            LocalDateTime expectedAt,
            String trackingNumber,
            String createdBy
    ) {
        ContractTrackingEventEntity event = new ContractTrackingEventEntity();
        event.setContract(contract);
        event.setEventType(eventType.name());
        event.setEventStatus(eventStatus);
        event.setTitle(title);
        event.setNote(normalizeNullable(note));
        event.setExpectedAt(expectedAt);
        event.setActualAt(LocalDateTime.now(APP_ZONE));
        event.setTrackingNumber(normalizeNullable(trackingNumber));
        event.setCreatedBy(createdBy);
        contractTrackingEventRepository.save(event);
    }

    private AuthenticatedUser requireViewer() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (!VIEW_ROLES.contains(normalizeUpper(currentUser.role()))) {
            throw new ForbiddenOperationException("You do not have permission to view sale orders");
        }
        return currentUser;
    }

    private AuthenticatedUser requireInternalOperator() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (!INTERNAL_ROLES.contains(normalizeUpper(currentUser.role()))) {
            throw new ForbiddenOperationException("You do not have permission to manage sale orders");
        }
        return currentUser;
    }

    private void requireAccountingOperator() {
        RoleName role = roleOf(currentUserProvider.getCurrentUser());
        if (role != RoleName.ACCOUNTANT && role != RoleName.OWNER) {
            throw new ForbiddenOperationException("Only accountant users can create invoices from sale orders");
        }
    }

    private CustomerProfileEntity loadCustomerForCurrentUser(AuthenticatedUser currentUser) {
        return customerProfileRepository.findByUser_Id(currentUser.userId()).orElseThrow(CustomerProfileNotFoundException::new);
    }

    private RoleName roleOf(AuthenticatedUser currentUser) {
        return RoleName.from(currentUser.role());
    }

    private void logAudit(String action, String entityId, Object oldValue, Object newValue, String userId) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType("SALE_ORDER");
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

    private BigDecimal sum(List<SaleOrderDetailResponseData.ItemData> items, java.util.function.Function<SaleOrderDetailResponseData.ItemData, BigDecimal> extractor) {
        BigDecimal total = ZERO;
        for (SaleOrderDetailResponseData.ItemData item : items) {
            total = total.add(normalizeQuantity(extractor.apply(item)));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private String saleOrderNumberOf(ContractEntity saleOrder) {
        return StringUtils.hasText(saleOrder.getSaleOrderNumber()) ? saleOrder.getSaleOrderNumber() : "SO-" + saleOrder.getContractNumber();
    }

    private String deriveInvoiceStatus(String storedStatus, BigDecimal paidAmount, BigDecimal grandTotal) {
        String normalized = normalizeInvoiceStatus(storedStatus);
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

    private String normalizeInvoiceStatus(String value) {
        String normalized = normalizeUpper(value);
        if (!StringUtils.hasText(normalized) || "OPEN".equals(normalized)) {
            return "ISSUED";
        }
        return normalized;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeQuantity(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
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

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private record InvoiceFinancialSummary(
            BigDecimal grandTotal,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,
            String status
    ) {
    }
}
