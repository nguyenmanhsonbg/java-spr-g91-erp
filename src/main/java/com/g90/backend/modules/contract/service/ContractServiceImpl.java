package com.g90.backend.modules.contract.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.ContractApprovalNotFoundException;
import com.g90.backend.exception.ContractCancelNotAllowedException;
import com.g90.backend.exception.ContractCreditLimitExceededException;
import com.g90.backend.exception.ContractDocumentNotFoundException;
import com.g90.backend.exception.ContractInventoryUnavailableException;
import com.g90.backend.exception.ContractNotEditableException;
import com.g90.backend.exception.ContractNotFoundException;
import com.g90.backend.exception.ContractPricingNotFoundException;
import com.g90.backend.exception.ContractSubmitNotAllowedException;
import com.g90.backend.exception.CustomerProfileNotFoundException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.QuotationAlreadyConvertedException;
import com.g90.backend.exception.QuotationNotFoundException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.contract.dto.ContractApprovalDecisionRequest;
import com.g90.backend.modules.contract.dto.ContractApprovalReviewResponseData;
import com.g90.backend.modules.contract.dto.ContractApprovalResponseData;
import com.g90.backend.modules.contract.dto.ContractCancelRequest;
import com.g90.backend.modules.contract.dto.ContractCreateRequest;
import com.g90.backend.modules.contract.dto.ContractDetailResponseData;
import com.g90.backend.modules.contract.dto.ContractFormInitQuery;
import com.g90.backend.modules.contract.dto.ContractFormInitResponseData;
import com.g90.backend.modules.contract.dto.ContractDocumentEmailRequest;
import com.g90.backend.modules.contract.dto.ContractDocumentExportRequest;
import com.g90.backend.modules.contract.dto.ContractDocumentGenerateRequest;
import com.g90.backend.modules.contract.dto.ContractDocumentListResponseData;
import com.g90.backend.modules.contract.dto.ContractFromQuotationResponseData;
import com.g90.backend.modules.contract.dto.ContractItemRequest;
import com.g90.backend.modules.contract.dto.ContractItemResponse;
import com.g90.backend.modules.contract.dto.ContractListQuery;
import com.g90.backend.modules.contract.dto.ContractListResponseData;
import com.g90.backend.modules.contract.dto.ContractPreviewRequest;
import com.g90.backend.modules.contract.dto.ContractPreviewResponseData;
import com.g90.backend.modules.contract.dto.ContractResponse;
import com.g90.backend.modules.contract.dto.ContractSubmitRequest;
import com.g90.backend.modules.contract.dto.ContractTrackingResponseData;
import com.g90.backend.modules.contract.dto.ContractUpdateRequest;
import com.g90.backend.modules.contract.dto.CreateContractFromQuotationRequest;
import com.g90.backend.modules.contract.dto.PendingContractApprovalListQuery;
import com.g90.backend.modules.contract.dto.PendingContractApprovalListResponseData;
import com.g90.backend.modules.contract.entity.ContractApprovalEntity;
import com.g90.backend.modules.contract.entity.ContractApprovalStatus;
import com.g90.backend.modules.contract.entity.ContractApprovalTier;
import com.g90.backend.modules.contract.entity.ContractApprovalType;
import com.g90.backend.modules.contract.entity.ContractCancellationReason;
import com.g90.backend.modules.contract.entity.ContractDocumentEntity;
import com.g90.backend.modules.contract.entity.ContractDocumentType;
import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.contract.entity.ContractItemEntity;
import com.g90.backend.modules.contract.entity.ContractPendingAction;
import com.g90.backend.modules.contract.entity.ContractStatus;
import com.g90.backend.modules.contract.entity.ContractStatusHistoryEntity;
import com.g90.backend.modules.contract.entity.ContractTrackingEventEntity;
import com.g90.backend.modules.contract.entity.ContractTrackingEventType;
import com.g90.backend.modules.contract.entity.ContractVersionEntity;
import com.g90.backend.modules.contract.integration.ContractCreditGateway;
import com.g90.backend.modules.contract.integration.ContractDocumentGateway;
import com.g90.backend.modules.contract.integration.ContractEmailGateway;
import com.g90.backend.modules.contract.integration.ContractInventoryGateway;
import com.g90.backend.modules.contract.integration.ContractNotificationGateway;
import com.g90.backend.modules.contract.integration.ContractPricingGateway;
import com.g90.backend.modules.contract.integration.ContractSchedulerSupport;
import com.g90.backend.modules.contract.mapper.ContractMapper;
import com.g90.backend.modules.contract.repository.ContractApprovalRepository;
import com.g90.backend.modules.contract.repository.ContractDocumentRepository;
import com.g90.backend.modules.contract.repository.ContractRepository;
import com.g90.backend.modules.contract.repository.ContractSpecifications;
import com.g90.backend.modules.contract.repository.ContractStatusHistoryRepository;
import com.g90.backend.modules.contract.repository.ContractTrackingEventRepository;
import com.g90.backend.modules.contract.repository.ContractVersionRepository;
import com.g90.backend.modules.debt.entity.DebtInvoiceEntity;
import com.g90.backend.modules.debt.repository.DebtInvoiceRepository;
import com.g90.backend.modules.debt.entity.PaymentAllocationEntity;
import com.g90.backend.modules.debt.repository.PaymentAllocationRepository;
import com.g90.backend.modules.payment.dto.PaymentOptionData;
import com.g90.backend.modules.payment.entity.InvoiceEntity;
import com.g90.backend.modules.payment.entity.InvoiceItemEntity;
import com.g90.backend.modules.payment.entity.PaymentOptionEntity;
import com.g90.backend.modules.payment.repository.InvoiceRepository;
import com.g90.backend.modules.payment.repository.PaymentOptionRepository;
import com.g90.backend.modules.product.dto.PaginationResponse;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.entity.ProductStatus;
import com.g90.backend.modules.product.repository.ProductRepository;
import com.g90.backend.modules.project.repository.ProjectInvoiceRepository;
import com.g90.backend.modules.quotation.entity.QuotationEntity;
import com.g90.backend.modules.quotation.entity.QuotationItemEntity;
import com.g90.backend.modules.quotation.entity.QuotationStatus;
import com.g90.backend.modules.quotation.repository.QuotationRepository;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ContractServiceImpl implements ContractService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal HIGH_VALUE_APPROVAL_THRESHOLD = new BigDecimal("500000000.00");
    private static final BigDecimal CANCELLATION_APPROVAL_THRESHOLD = new BigDecimal("100000000.00");
    private static final BigDecimal MINIMUM_ALLOWED_PRICE_FACTOR = new BigDecimal("0.90");
    private static final BigDecimal TEN_PERCENT = new BigDecimal("10.00");
    private static final BigDecimal THIRTY_PERCENT = new BigDecimal("30.00");
    private static final String STANDARD_PAYMENT_TERMS = "70% on delivery, 30% within 30 days";
    private static final String PROFITABILITY_TODO_NOTE = "TODO: integrate cost and margin aggregator for full profitability review.";
    private static final Set<String> CUSTOMER_LOADING_STATUSES = Set.of(
            ContractStatus.PICKED.name(),
            ContractStatus.IN_TRANSIT.name()
    );
    private static final Set<String> PREPARATION_STARTED_STATUSES = Set.of(
            ContractStatus.PROCESSING.name(),
            ContractStatus.RESERVED.name(),
            ContractStatus.PICKED.name(),
            ContractStatus.IN_TRANSIT.name(),
            ContractStatus.DELIVERED.name(),
            ContractStatus.COMPLETED.name()
    );
    private static final Set<String> CONTRACT_SORT_FIELDS = Set.of(
            "createdAt",
            "contractNumber",
            "totalAmount",
            "expectedDeliveryDate",
            "status",
            "approvalStatus"
    );
    private static final Set<String> PENDING_APPROVAL_SORT_FIELDS = Set.of(
            "approvalRequestedAt",
            "totalAmount",
            "contractNumber",
            "submittedAt"
    );

    private final ContractRepository contractRepository;
    private final ContractVersionRepository contractVersionRepository;
    private final ContractApprovalRepository contractApprovalRepository;
    private final ContractStatusHistoryRepository contractStatusHistoryRepository;
    private final ContractDocumentRepository contractDocumentRepository;
    private final ContractTrackingEventRepository contractTrackingEventRepository;
    private final QuotationRepository quotationRepository;
    private final ProductRepository productRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ProjectInvoiceRepository projectInvoiceRepository;
    private final AuditLogRepository auditLogRepository;
    private final InvoiceRepository invoiceRepository;
    private final DebtInvoiceRepository debtInvoiceRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PaymentOptionRepository paymentOptionRepository;
    private final ContractPricingGateway contractPricingGateway;
    private final ContractCreditGateway contractCreditGateway;
    private final ContractInventoryGateway contractInventoryGateway;
    private final ContractNotificationGateway contractNotificationGateway;
    private final ContractDocumentGateway contractDocumentGateway;
    private final ContractEmailGateway contractEmailGateway;
    private final ContractSchedulerSupport contractSchedulerSupport;
    private final ContractMapper contractMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public ContractServiceImpl(
            ContractRepository contractRepository,
            ContractVersionRepository contractVersionRepository,
            ContractApprovalRepository contractApprovalRepository,
            ContractStatusHistoryRepository contractStatusHistoryRepository,
            ContractDocumentRepository contractDocumentRepository,
            ContractTrackingEventRepository contractTrackingEventRepository,
            QuotationRepository quotationRepository,
            ProductRepository productRepository,
            CustomerProfileRepository customerProfileRepository,
            ProjectInvoiceRepository projectInvoiceRepository,
            AuditLogRepository auditLogRepository,
            InvoiceRepository invoiceRepository,
            DebtInvoiceRepository debtInvoiceRepository,
            PaymentAllocationRepository paymentAllocationRepository,
            PaymentOptionRepository paymentOptionRepository,
            ContractPricingGateway contractPricingGateway,
            ContractCreditGateway contractCreditGateway,
            ContractInventoryGateway contractInventoryGateway,
            ContractNotificationGateway contractNotificationGateway,
            ContractDocumentGateway contractDocumentGateway,
            ContractEmailGateway contractEmailGateway,
            ContractSchedulerSupport contractSchedulerSupport,
            ContractMapper contractMapper,
            CurrentUserProvider currentUserProvider,
            ObjectMapper objectMapper
    ) {
        this.contractRepository = contractRepository;
        this.contractVersionRepository = contractVersionRepository;
        this.contractApprovalRepository = contractApprovalRepository;
        this.contractStatusHistoryRepository = contractStatusHistoryRepository;
        this.contractDocumentRepository = contractDocumentRepository;
        this.contractTrackingEventRepository = contractTrackingEventRepository;
        this.quotationRepository = quotationRepository;
        this.productRepository = productRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.projectInvoiceRepository = projectInvoiceRepository;
        this.auditLogRepository = auditLogRepository;
        this.invoiceRepository = invoiceRepository;
        this.debtInvoiceRepository = debtInvoiceRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.paymentOptionRepository = paymentOptionRepository;
        this.contractPricingGateway = contractPricingGateway;
        this.contractCreditGateway = contractCreditGateway;
        this.contractInventoryGateway = contractInventoryGateway;
        this.contractNotificationGateway = contractNotificationGateway;
        this.contractDocumentGateway = contractDocumentGateway;
        this.contractEmailGateway = contractEmailGateway;
        this.contractSchedulerSupport = contractSchedulerSupport;
        this.contractMapper = contractMapper;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ContractFormInitResponseData getContractFormInit(ContractFormInitQuery query) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        ensureInternalRole(currentUser);

        if (!StringUtils.hasText(query.getCustomerId()) && !StringUtils.hasText(query.getQuotationId())) {
            throw RequestValidationException.singleError("customerId", "Customer ID or quotation ID is required");
        }

        QuotationEntity quotation = null;
        CustomerProfileEntity customer;
        if (StringUtils.hasText(query.getQuotationId())) {
            quotation = quotationRepository.findDetailedById(query.getQuotationId().trim())
                    .orElseThrow(QuotationNotFoundException::new);
            if (StringUtils.hasText(query.getCustomerId())
                    && !quotation.getCustomer().getId().equals(query.getCustomerId().trim())) {
                throw RequestValidationException.singleError("quotationId", "Quotation does not belong to selected customer");
            }
            customer = loadCustomer(quotation.getCustomer().getId());
            validateQuotationForContractUse(quotation, customer.getId());
        } else {
            customer = loadCustomer(query.getCustomerId());
        }

        ContractCreditGateway.CreditSnapshot creditSnapshot = contractCreditGateway.getCreditSnapshot(customer);
        BigDecimal depositPercentage = determineDepositPercentage(customer);
        List<ContractFormInitResponseData.ItemData> items = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (quotation != null && quotation.getItems() != null && !quotation.getItems().isEmpty()) {
            Map<String, ContractPricingGateway.PricingData> pricingByProductId = contractPricingGateway.resolveBasePrices(
                    customer,
                    quotation.getItems().stream().map(item -> item.getProduct().getId()).toList()
            );
            for (QuotationItemEntity quotationItem : quotation.getItems()) {
                ProductEntity product = quotationItem.getProduct();
                ContractPricingGateway.PricingData pricing = pricingByProductId.get(product.getId());
                BigDecimal baseUnitPrice = pricing == null ? null : normalizeMoney(pricing.baseUnitPrice());
                if (pricing == null || pricing.baseUnitPrice() == null) {
                    warnings.add("No active base price found for product " + product.getProductCode());
                }
                if (!ProductStatus.ACTIVE.name().equalsIgnoreCase(product.getStatus())) {
                    warnings.add("Product " + product.getProductCode() + " is no longer ACTIVE");
                }
                items.add(new ContractFormInitResponseData.ItemData(
                        product.getId(),
                        product.getProductCode(),
                        product.getProductName(),
                        product.getUnit(),
                        quotationItem.getQuantity(),
                        quotationItem.getUnitPrice(),
                        baseUnitPrice,
                        quotationItem.getUnitPrice(),
                        quotationItem.getTotalPrice()
                ));
            }
            if (quotation.getValidUntil() != null && !quotation.getValidUntil().isAfter(LocalDate.now(APP_ZONE).plusDays(2))) {
                warnings.add("Quotation validity is nearing expiry");
            }
        }

        if (creditSnapshot.exceeded()) {
            warnings.add("Customer current debt already exceeds credit limit");
        }

        return new ContractFormInitResponseData(
                new ContractFormInitResponseData.CustomerData(
                        customer.getId(),
                        customer.getCompanyName(),
                        customer.getCustomerType(),
                        customer.getContactPerson(),
                        customer.getPhone(),
                        customer.getEmail(),
                        customer.getAddress(),
                        creditSnapshot.creditLimit(),
                        creditSnapshot.currentDebt(),
                        creditSnapshot.availableCredit(),
                        depositPercentage
                ),
                quotation == null
                        ? null
                        : new ContractFormInitResponseData.QuotationData(
                                quotation.getId(),
                                quotation.getQuotationNumber(),
                                quotation.getStatus(),
                                quotation.getValidUntil(),
                                quotation.getProject() == null ? null : quotation.getProject().getId(),
                                quotation.getProject() == null ? null : quotation.getProject().getProjectCode(),
                                quotation.getProject() == null ? null : quotation.getProject().getName(),
                                quotation.getDeliveryRequirement(),
                                quotation.getNote(),
                                contractMapper.toPaymentOptionData(quotation.getPaymentOption())
                        ),
                new ContractFormInitResponseData.DefaultsData(
                        resolveCustomerPaymentTerms(customer),
                        customer.getAddress(),
                        quotation == null ? null : quotation.getDeliveryRequirement(),
                        quotation == null ? null : contractMapper.toPaymentOptionData(quotation.getPaymentOption())
                ),
                items,
                warnings,
                loadAvailablePaymentOptions()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ContractPreviewResponseData previewContract(ContractPreviewRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        ensureInternalRole(currentUser);
        PreparedContract prepared = prepareContract(request, false);
        return toPreviewResponse(prepared);
    }

    @Override
    @Transactional
    public ContractDetailResponseData createContract(ContractCreateRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        ensureInternalRole(currentUser);

        PreparedContract prepared = prepareContract(request, true);
        if (prepared.quotation() != null && contractRepository.existsByQuotation_Id(prepared.quotation().getId())) {
            throw new QuotationAlreadyConvertedException();
        }

        ContractEntity contract = new ContractEntity();
        applyPreparedContract(contract, prepared, currentUser.userId(), generateContractNumber(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        contract.setStatus(ContractStatus.DRAFT.name());

        ContractEntity saved = contractRepository.save(contract);
        recordVersion(saved, "Initial contract draft created", currentUser.userId());
        recordStatusHistory(saved, null, ContractStatus.DRAFT.name(), "Contract draft created", currentUser.userId());
        recordTrackingEvent(saved, ContractTrackingEventType.DRAFT_CREATED, ContractStatus.DRAFT.name(), "Draft created", "Contract draft created", null, null, currentUser.userId());

        if (prepared.quotation() != null) {
            markQuotationConverted(prepared.quotation(), saved, currentUser.userId());
        }

        ContractEntity detailed = loadDetailedContract(saved.getId());
        contractNotificationGateway.notifyContractCreated(detailed, "New contract draft created");
        ContractDetailResponseData response = toDetailResponse(detailed);
        logAudit("CREATE_CONTRACT", detailed.getId(), null, response, currentUser.userId());
        return response;
    }

    @Override
    @Transactional
    public ContractFromQuotationResponseData createFromQuotation(String quotationId, CreateContractFromQuotationRequest request) {
        QuotationEntity quotation = quotationRepository.findDetailedById(quotationId)
                .orElseThrow(QuotationNotFoundException::new);

        ContractCreateRequest createRequest = new ContractCreateRequest();
        createRequest.setCustomerId(quotation.getCustomer().getId());
        createRequest.setQuotationId(quotationId);
        createRequest.setPaymentTerms(request.getPaymentTerms());
        createRequest.setPaymentOptionCode(StringUtils.hasText(request.getPaymentOptionCode())
                ? request.getPaymentOptionCode()
                : quotation.getPaymentOption() == null ? null : quotation.getPaymentOption().getCode());
        createRequest.setDeliveryAddress(request.getDeliveryAddress());
        createRequest.setDeliveryTerms(quotation.getDeliveryRequirement());
        createRequest.setNote(quotation.getNote());
        ContractDetailResponseData created = createContract(createRequest);

        return new ContractFromQuotationResponseData(
                new ContractFromQuotationResponseData.ContractData(
                        created.contract().id(),
                        created.contract().contractNumber(),
                        created.contract().customerId(),
                        created.contract().quotationId(),
                        created.contract().totalAmount(),
                        created.contract().status(),
                        created.contract().paymentTerms(),
                        created.contract().paymentOption(),
                        created.contract().deliveryAddress(),
                        created.contract().createdAt()
                ),
                new ContractFromQuotationResponseData.QuotationData(
                        quotation.getId(),
                        quotation.getQuotationNumber(),
                        QuotationStatus.CONVERTED.name()
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ContractListResponseData getContracts(ContractListQuery query) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        String scopedCustomerId = null;
        if (RoleName.CUSTOMER.name().equalsIgnoreCase(currentUser.role())) {
            scopedCustomerId = loadCurrentCustomer().getId();
        } else {
            ensureInternalRole(currentUser);
        }

        Page<ContractEntity> contracts = contractRepository.findAll(
                ContractSpecifications.byQuery(query, scopedCustomerId),
                PageRequest.of(normalizePage(query.getPage()) - 1, normalizePageSize(query.getPageSize()), buildContractSort(query))
        );

        return new ContractListResponseData(
                contracts.stream().map(contractMapper::toListItem).toList(),
                PaginationResponse.builder()
                        .page(contracts.getNumber() + 1)
                        .pageSize(contracts.getSize())
                        .totalItems(contracts.getTotalElements())
                        .totalPages(contracts.getTotalPages())
                        .build(),
                new ContractListResponseData.Filters(
                        normalizeNullable(query.getContractNumber()),
                        normalizeNullable(scopedCustomerId != null ? scopedCustomerId : query.getCustomerId()),
                        normalizeNullable(query.getStatus()),
                        normalizeNullable(query.getApprovalStatus()),
                        query.getCreatedFrom(),
                        query.getCreatedTo(),
                        query.getDeliveryFrom(),
                        query.getDeliveryTo(),
                        query.getConfidential(),
                        query.getSubmitted()
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ContractDetailResponseData getContractDetail(String contractId) {
        return toDetailResponse(loadAccessibleContract(contractId, currentUserProvider.getCurrentUser()));
    }

    @Override
    @Transactional
    public ContractDetailResponseData updateContract(String contractId, ContractUpdateRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        ensureInternalRole(currentUser);

        ContractEntity contract = loadDetailedContract(contractId);
        if (!ContractStatus.DRAFT.name().equalsIgnoreCase(contract.getStatus())) {
            throw new ContractNotEditableException();
        }
        if (!contract.getCustomer().getId().equals(request.getCustomerId().trim())) {
            throw RequestValidationException.singleError("customerId", "Customer cannot be changed once contract draft is created");
        }

        ContractPreviewRequest normalizedRequest = normalizedUpdateRequest(contract, request);
        PreparedContract prepared = prepareContract(normalizedRequest, true);
        BigDecimal priceChangePercent = computePriceChangePercent(contract, normalizedRequest.getItems());

        applyPreparedContract(contract, prepared, currentUser.userId(), contract.getContractNumber(), priceChangePercent);
        contract.setUpdatedBy(currentUser.userId());
        ContractEntity saved = contractRepository.save(contract);

        recordVersion(saved, request.getChangeReason().trim(), currentUser.userId());
        recordTrackingEvent(saved, ContractTrackingEventType.UPDATED, saved.getStatus(), "Contract updated", request.getChangeReason().trim(), null, null, currentUser.userId());
        ContractDetailResponseData response = toDetailResponse(loadDetailedContract(saved.getId()));
        logAudit("UPDATE_CONTRACT", saved.getId(), null, response, currentUser.userId());
        return response;
    }

    @Override
    @Transactional
    public ContractApprovalResponseData cancelContract(String contractId, ContractCancelRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        ensureInternalRole(currentUser);

        ContractEntity contract = loadDetailedContract(contractId);
        ensureCancellable(contract);
        String cancellationReasonCode = request.getCancellationReason().name();
        String cancellationNote = normalizeNullable(request.getCancellationNote());
        contract.setCancellationReasonCode(cancellationReasonCode);
        contract.setCancellationNote(cancellationNote);
        contract.setUpdatedBy(currentUser.userId());

        if (requiresCancellationApproval(contract) && !RoleName.OWNER.name().equalsIgnoreCase(currentUser.role())) {
            String currentStatus = contract.getStatus();
            createApprovalRequest(
                    contract,
                    ContractApprovalType.CANCELLATION,
                    ContractApprovalTier.MANAGER,
                    ContractPendingAction.CANCEL,
                    cancellationReasonCode,
                    currentUser.userId()
            );
            contract.setStatus(currentStatus);
            contractRepository.save(contract);
            recordTrackingEvent(contract, ContractTrackingEventType.CANCEL_REQUESTED, contract.getStatus(), "Cancellation approval requested", cancellationReasonCode, null, null, currentUser.userId());
            logAudit("REQUEST_CONTRACT_CANCELLATION", contract.getId(), null, cancellationReasonCode, currentUser.userId());
            return new ContractApprovalResponseData(contract.getId(), contract.getContractNumber(), contract.getApprovalStatus(), contract.getStatus(), "PENDING_APPROVAL", currentUser.userId(), contract.getApprovalRequestedAt(), cancellationReasonCode);
        }

        String previousStatus = contract.getStatus();
        contract.setApprovalStatus(ContractApprovalStatus.NOT_REQUIRED.name());
        ContractEntity saved = finalizeCancellation(contract, cancellationReasonCode, cancellationNote, currentUser.userId());

        recordStatusHistory(saved, previousStatus, ContractStatus.CANCELLED.name(), cancellationReasonCode, currentUser.userId());
        recordTrackingEvent(saved, ContractTrackingEventType.CANCELLED, ContractStatus.CANCELLED.name(), "Contract cancelled", cancellationReasonCode, null, null, currentUser.userId());
        contractNotificationGateway.notifyCancellation(saved, cancellationReasonCode);
        logAudit("CANCEL_CONTRACT", saved.getId(), previousStatus, saved.getStatus(), currentUser.userId());
        return new ContractApprovalResponseData(saved.getId(), saved.getContractNumber(), saved.getApprovalStatus(), saved.getStatus(), "CANCELLED", currentUser.userId(), saved.getCancelledAt(), cancellationReasonCode);
    }

    @Override
    @Transactional
    public ContractApprovalResponseData submitContract(String contractId, ContractSubmitRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        ensureInternalRole(currentUser);

        ContractEntity contract = loadDetailedContract(contractId);
        if (ContractStatus.DRAFT.name().equalsIgnoreCase(contract.getStatus())) {
            validateContractReadyForSubmission(contract);
            String previousStatus = contract.getStatus();
            contract.setStatus(ContractStatus.PENDING_CUSTOMER_APPROVAL.name());
            contract.setPendingAction(null);
            contract.setLastStatusChangeAt(LocalDateTime.now(APP_ZONE));
            contract.setUpdatedBy(currentUser.userId());
            ContractEntity saved = contractRepository.save(contract);

            recordStatusHistory(saved, previousStatus, ContractStatus.PENDING_CUSTOMER_APPROVAL.name(), normalizeNullable(request.getSubmissionNote()), currentUser.userId());
            recordTrackingEvent(
                    saved,
                    ContractTrackingEventType.CUSTOMER_APPROVAL_REQUESTED,
                    ContractStatus.PENDING_CUSTOMER_APPROVAL.name(),
                    "Customer approval requested",
                    normalizeNullable(request.getSubmissionNote()),
                    null,
                    null,
                    currentUser.userId()
            );
            logAudit("REQUEST_CUSTOMER_APPROVAL", saved.getId(), previousStatus, saved.getStatus(), currentUser.userId());
            return new ContractApprovalResponseData(
                    saved.getId(),
                    saved.getContractNumber(),
                    saved.getApprovalStatus(),
                    saved.getStatus(),
                    ContractStatus.PENDING_CUSTOMER_APPROVAL.name(),
                    currentUser.userId(),
                    saved.getLastStatusChangeAt(),
                    normalizeNullable(request.getSubmissionNote())
            );
        }

        if (!ContractStatus.CUSTOMER_APPROVAL.name().equalsIgnoreCase(contract.getStatus())) {
            throw new ContractSubmitNotAllowedException("Only draft or customer-approved contract can be progressed");
        }

        if (request.getScheduledSubmissionAt() != null && request.getScheduledSubmissionAt().isAfter(LocalDateTime.now(APP_ZONE))) {
            contract.setAutoSubmitDueAt(request.getScheduledSubmissionAt());
            contract.setUpdatedBy(currentUser.userId());
            contractRepository.save(contract);
            logAudit("SCHEDULE_CONTRACT_SUBMISSION", contract.getId(), null, request.getScheduledSubmissionAt(), currentUser.userId());
            return new ContractApprovalResponseData(contract.getId(), contract.getContractNumber(), contract.getApprovalStatus(), contract.getStatus(), "SCHEDULED", currentUser.userId(), request.getScheduledSubmissionAt(), normalizeNullable(request.getSubmissionNote()));
        }

        validateContractReadyForSubmission(contract);
        enforceCreditLimit(contract);
        assertInventoryAvailable(contract);

        String previousStatus = contract.getStatus();
        if (contract.isRequiresApproval()) {
            String approvalReason = defaultIfNull(contract.getTotalAmount()).compareTo(HIGH_VALUE_APPROVAL_THRESHOLD) > 0
                    ? "Contract value exceeds owner approval threshold"
                    : "Price override requires approval";
            createApprovalRequest(
                    contract,
                    defaultIfNull(contract.getTotalAmount()).compareTo(HIGH_VALUE_APPROVAL_THRESHOLD) > 0 ? ContractApprovalType.SUBMISSION : ContractApprovalType.PRICE_OVERRIDE,
                    ContractApprovalTier.OWNER,
                    ContractPendingAction.SUBMIT,
                    approvalReason,
                    currentUser.userId()
            );
            recordStatusHistory(contract, previousStatus, ContractStatus.PENDING_APPROVAL.name(), approvalReason, currentUser.userId());
            recordTrackingEvent(contract, ContractTrackingEventType.APPROVAL_REQUESTED, ContractStatus.PENDING_APPROVAL.name(), "Approval requested", approvalReason, null, null, currentUser.userId());
            contractNotificationGateway.notifyApprovalRequested(contract, ContractApprovalType.SUBMISSION, approvalReason);
            logAudit("REQUEST_CONTRACT_APPROVAL", contract.getId(), previousStatus, ContractStatus.PENDING_APPROVAL.name(), currentUser.userId());
            return new ContractApprovalResponseData(contract.getId(), contract.getContractNumber(), contract.getApprovalStatus(), contract.getStatus(), "PENDING_APPROVAL", currentUser.userId(), contract.getApprovalRequestedAt(), approvalReason);
        }

        contract.setApprovalStatus(ContractApprovalStatus.NOT_REQUIRED.name());
        prepareSubmittedContract(contract, currentUser.userId());
        ContractEntity saved = contractRepository.save(contract);

        recordStatusHistory(saved, previousStatus, ContractStatus.SUBMITTED.name(), normalizeNullable(request.getSubmissionNote()), currentUser.userId());
        recordTrackingEvent(saved, ContractTrackingEventType.SUBMITTED, ContractStatus.SUBMITTED.name(), "Contract submitted", normalizeNullable(request.getSubmissionNote()), null, null, currentUser.userId());
        contractInventoryGateway.reserveInventory(saved);
        recordTrackingEvent(saved, ContractTrackingEventType.INVENTORY_RESERVED, ContractStatus.SUBMITTED.name(), "Inventory reserved", "Reservation requested for fulfillment", null, null, currentUser.userId());
        contractNotificationGateway.notifyWarehousePreparation(saved, "Contract submitted for preparation");
        logAudit("SUBMIT_CONTRACT", saved.getId(), previousStatus, saved.getStatus(), currentUser.userId());
        return new ContractApprovalResponseData(saved.getId(), saved.getContractNumber(), saved.getApprovalStatus(), saved.getStatus(), "SUBMITTED", currentUser.userId(), saved.getSubmittedAt(), normalizeNullable(request.getSubmissionNote()));
    }

    @Override
    @Transactional
    public ContractApprovalResponseData approveByCustomer(String contractId, ContractApprovalDecisionRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        ensureCustomerRole(currentUser);

        ContractEntity contract = loadAccessibleContract(contractId, currentUser);
        if (!ContractStatus.PENDING_CUSTOMER_APPROVAL.name().equalsIgnoreCase(contract.getStatus())) {
            throw new ContractSubmitNotAllowedException("Only contract pending customer approval can be approved by customer");
        }

        String previousStatus = contract.getStatus();
        contract.setStatus(ContractStatus.CUSTOMER_APPROVAL.name());
        contract.setLastStatusChangeAt(LocalDateTime.now(APP_ZONE));
        contract.setUpdatedBy(currentUser.userId());
        ContractEntity saved = contractRepository.save(contract);

        recordStatusHistory(saved, previousStatus, ContractStatus.CUSTOMER_APPROVAL.name(), normalizeNullable(request.getComment()), currentUser.userId());
        recordTrackingEvent(
                saved,
                ContractTrackingEventType.CUSTOMER_APPROVED,
                ContractStatus.CUSTOMER_APPROVAL.name(),
                "Customer approved contract",
                normalizeNullable(request.getComment()),
                null,
                null,
                currentUser.userId()
        );
        contractNotificationGateway.notifyApprovalDecision(saved, "CUSTOMER_APPROVED", "Customer approved contract");
        logAudit("CUSTOMER_APPROVE_CONTRACT", saved.getId(), previousStatus, saved.getStatus(), currentUser.userId());
        return new ContractApprovalResponseData(
                saved.getId(),
                saved.getContractNumber(),
                saved.getApprovalStatus(),
                saved.getStatus(),
                "CUSTOMER_APPROVED",
                currentUser.userId(),
                saved.getLastStatusChangeAt(),
                normalizeNullable(request.getComment())
        );
    }

    @Override
    @Transactional
    public ContractApprovalResponseData rejectByCustomer(String contractId, ContractApprovalDecisionRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        ensureCustomerRole(currentUser);

        ContractEntity contract = loadAccessibleContract(contractId, currentUser);
        if (!ContractStatus.PENDING_CUSTOMER_APPROVAL.name().equalsIgnoreCase(contract.getStatus())) {
            throw new ContractSubmitNotAllowedException("Only contract pending customer approval can be rejected by customer");
        }

        return moveBackToDraft(
                contract,
                "CUSTOMER_REJECTED",
                ContractTrackingEventType.CUSTOMER_REJECTED,
                "Customer rejected contract",
                normalizeNullable(request.getComment()),
                currentUser.userId(),
                "CUSTOMER_REJECT_CONTRACT"
        );
    }

    @Override
    @Transactional
    public ContractApprovalResponseData rejectCustomerApproval(String contractId, ContractApprovalDecisionRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        ensureInternalRole(currentUser);

        ContractEntity contract = loadDetailedContract(contractId);
        if (!ContractStatus.CUSTOMER_APPROVAL.name().equalsIgnoreCase(contract.getStatus())) {
            throw new ContractSubmitNotAllowedException("Only customer-approved contract can be rejected by accountant");
        }

        contract.setUpdatedBy(currentUser.userId());
        return moveBackToDraft(
                contract,
                "ACCOUNTANT_REJECTED",
                ContractTrackingEventType.ACCOUNTANT_REJECTED,
                "Accountant rejected contract",
                normalizeNullable(request.getComment()),
                currentUser.userId(),
                "ACCOUNTANT_REJECT_CONTRACT"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ContractTrackingResponseData getTracking(String contractId) {
        ContractEntity contract = loadAccessibleContract(contractId, currentUserProvider.getCurrentUser());
        return new ContractTrackingResponseData(
                contract.getId(),
                contract.getContractNumber(),
                contract.getStatus(),
                contract.getLastTrackingRefreshAt(),
                4,
                contract.getTrackingEvents().stream().map(contractMapper::toTrackingEvent).toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ContractDocumentListResponseData getDocuments(String contractId) {
        ContractEntity contract = loadAccessibleContract(contractId, currentUserProvider.getCurrentUser());
        ensureCustomerDocumentAccess(contract, currentUserProvider.getCurrentUser(), false);
        return new ContractDocumentListResponseData(
                contract.getId(),
                contract.getContractNumber(),
                contract.getDocuments().stream().map(contractMapper::toDocumentData).toList()
        );
    }

    @Override
    @Transactional
    public ContractDetailResponseData.DocumentData generateDocument(String contractId, ContractDocumentGenerateRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        ensureInternalRole(currentUser);
        ContractEntity contract = loadDetailedContract(contractId);

        boolean officialDocument = Boolean.TRUE.equals(request.getOfficialDocument())
                && !ContractStatus.DRAFT.name().equalsIgnoreCase(contract.getStatus());
        long sequence = officialDocument ? nextDocumentSequence(request.getDocumentType()) : 0L;
        ContractDocumentGateway.DocumentDescriptor descriptor = contractDocumentGateway.generatePreview(contract, request.getDocumentType(), officialDocument, sequence);

        ContractDocumentEntity document = new ContractDocumentEntity();
        document.setContract(contract);
        document.setDocumentType(request.getDocumentType().name());
        document.setDocumentNumber(descriptor.documentNumber());
        document.setFileName(descriptor.fileName());
        document.setFileUrl(descriptor.fileUrl());
        document.setPreviewOnly(!officialDocument);
        document.setOfficialDocument(officialDocument);
        document.setWatermarkText(descriptor.watermarkText());
        document.setGeneratedBy(currentUser.userId());
        ContractDocumentEntity saved = contractDocumentRepository.save(document);

        recordTrackingEvent(contract, ContractTrackingEventType.DOCUMENT_GENERATED, contract.getStatus(), "Document generated", request.getDocumentType().name(), null, null, currentUser.userId());
        logAudit("GENERATE_CONTRACT_DOCUMENT", contract.getId(), null, saved.getDocumentType(), currentUser.userId());
        return contractMapper.toDocumentData(saved);
    }

    @Override
    @Transactional
    public ContractDetailResponseData.DocumentData exportDocument(String contractId, String documentId, ContractDocumentExportRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        ContractEntity contract = loadAccessibleContract(contractId, currentUser);
        ensureCustomerDocumentAccess(contract, currentUser, false);

        ContractDocumentEntity document = loadDocument(contract, documentId);
        document.setExportCount(Optional.ofNullable(document.getExportCount()).orElse(0) + 1);
        document.setLastExportedBy(currentUser.userId());
        document.setLastExportedAt(LocalDateTime.now(APP_ZONE));
        ContractDocumentEntity saved = contractDocumentRepository.save(document);

        recordTrackingEvent(contract, ContractTrackingEventType.DOCUMENT_EXPORTED, contract.getStatus(), "Document exported", normalizeNullable(request.getRequestedFormat()), null, null, currentUser.userId());
        logAudit("EXPORT_CONTRACT_DOCUMENT", contract.getId(), documentId, request.getRequestedFormat(), currentUser.userId());
        return contractMapper.toDocumentData(saved);
    }

    @Override
    @Transactional
    public ContractDetailResponseData.DocumentData emailDocument(String contractId, String documentId, ContractDocumentEmailRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        ContractEntity contract = loadAccessibleContract(contractId, currentUser);
        ensureCustomerDocumentAccess(contract, currentUser, false);

        ContractDocumentEntity document = loadDocument(contract, documentId);
        contractEmailGateway.sendDocument(document, request.getRecipientEmail().trim());
        document.setEmailedBy(currentUser.userId());
        document.setEmailedAt(LocalDateTime.now(APP_ZONE));
        ContractDocumentEntity saved = contractDocumentRepository.save(document);

        recordTrackingEvent(contract, ContractTrackingEventType.DOCUMENT_EMAILED, contract.getStatus(), "Document emailed", request.getRecipientEmail().trim(), null, null, currentUser.userId());
        logAudit("EMAIL_CONTRACT_DOCUMENT", contract.getId(), documentId, request.getRecipientEmail().trim(), currentUser.userId());
        return contractMapper.toDocumentData(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PendingContractApprovalListResponseData getPendingApprovals(PendingContractApprovalListQuery query) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (!RoleName.OWNER.name().equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenOperationException("You do not have permission to perform this action");
        }
        if (query.getMinAmount() != null && query.getMaxAmount() != null
                && query.getMinAmount().compareTo(query.getMaxAmount()) > 0) {
            throw RequestValidationException.singleError("minAmount", "Minimum amount must be less than or equal to maximum amount");
        }

        int page = normalizePage(query.getPage());
        int pageSize = normalizePageSize(query.getPageSize());
        Page<ContractEntity> pageResult = contractRepository.findAll(
                ContractSpecifications.byPendingApprovalQuery(query),
                PageRequest.of(page - 1, pageSize, buildPendingApprovalSort(query))
        );

        return new PendingContractApprovalListResponseData(
                pageResult.stream()
                        .map(contract -> {
                            ContractApprovalEntity approval = findRelevantApproval(contract).orElse(null);
                            return new PendingContractApprovalListResponseData.Item(
                                    contract.getId(),
                                    contract.getContractNumber(),
                                    contract.getCustomer() == null ? null : contract.getCustomer().getId(),
                                    contract.getCustomer() == null ? null : contract.getCustomer().getCompanyName(),
                                    contract.getTotalAmount(),
                                    contract.getApprovalStatus(),
                                    contract.getApprovalTier(),
                                    contract.getPendingAction(),
                                    approval == null ? contract.getCreatedBy() : approval.getRequestedBy(),
                                    contract.getApprovalRequestedAt(),
                                    contract.getApprovalDueAt(),
                                    contract.getSubmittedAt()
                            );
                        })
                        .toList(),
                PaginationResponse.builder()
                        .page(pageResult.getNumber() + 1)
                        .pageSize(pageResult.getSize())
                        .totalItems(pageResult.getTotalElements())
                        .totalPages(pageResult.getTotalPages())
                        .build(),
                new PendingContractApprovalListResponseData.Filters(
                        normalizeNullable(query.getKeyword()),
                        normalizeNullable(query.getCustomerId()),
                        normalizeNullable(query.getPendingAction()),
                        normalizeNullable(query.getApprovalTier()),
                        normalizeMoney(query.getMinAmount()),
                        normalizeMoney(query.getMaxAmount()),
                        query.getRequestedFrom() == null ? null : query.getRequestedFrom().atStartOfDay(),
                        query.getRequestedTo() == null ? null : query.getRequestedTo().plusDays(1).atStartOfDay().minusNanos(1)
                ),
                pageResult.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ContractApprovalReviewResponseData getApprovalReview(String contractId) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (!RoleName.OWNER.name().equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenOperationException("You do not have permission to perform this action");
        }

        ContractEntity contract = loadDetailedContract(contractId);
        ContractApprovalEntity approval = findRelevantApproval(contract).orElseThrow(ContractApprovalNotFoundException::new);
        ContractDetailResponseData detail = toDetailResponse(contract);
        ContractCreditGateway.CreditSnapshot creditSnapshot = contractCreditGateway.getCreditSnapshot(contract.getCustomer());
        BigDecimal projectedDebt = creditSnapshot.currentDebt().add(defaultIfNull(contract.getTotalAmount())).setScale(2, RoundingMode.HALF_UP);

        return new ContractApprovalReviewResponseData(
                detail,
                new ContractApprovalReviewResponseData.ApprovalRequestData(
                        approval.getId(),
                        approval.getApprovalType(),
                        approval.getApprovalTier(),
                        approval.getStatus(),
                        contract.getPendingAction(),
                        approval.getRequestedBy(),
                        approval.getRequestedAt(),
                        approval.getDueAt(),
                        approval.getComment()
                ),
                new ContractApprovalReviewResponseData.ReviewInsights(
                        buildApprovalReasons(contract, approval, creditSnapshot, projectedDebt),
                        customerHistoryMonths(contract.getCustomer()),
                        determineCreditRiskLevel(creditSnapshot, projectedDebt),
                        defaultIfNull(contract.getPriceChangePercent()),
                        creditSnapshot.creditLimit(),
                        creditSnapshot.currentDebt(),
                        projectedDebt,
                        creditSnapshot.availableCredit(),
                        PROFITABILITY_TODO_NOTE,
                        determineApprovalActionRecommendation(contract)
                )
        );
    }

    @Override
    @Transactional
    public ContractApprovalResponseData approveContract(String contractId, ContractApprovalDecisionRequest request) {
        return decideApproval(contractId, request, ContractApprovalStatus.APPROVED, "APPROVED");
    }

    @Override
    @Transactional
    public ContractApprovalResponseData rejectContract(String contractId, ContractApprovalDecisionRequest request) {
        return decideApproval(contractId, request, ContractApprovalStatus.REJECTED, "REJECTED");
    }

    @Override
    @Transactional
    public ContractApprovalResponseData requestModification(String contractId, ContractApprovalDecisionRequest request) {
        return decideApproval(contractId, request, ContractApprovalStatus.MODIFICATION_REQUESTED, "MODIFICATION_REQUESTED");
    }

    private PreparedContract prepareContract(ContractPreviewRequest request, boolean enforceCreditLimit) {
        CustomerProfileEntity customer = loadCustomer(request.getCustomerId());
        QuotationEntity quotation = resolveQuotation(request.getQuotationId(), customer.getId());
        PaymentOptionEntity paymentOption = resolvePaymentOption(
                StringUtils.hasText(request.getPaymentOptionCode())
                        ? request.getPaymentOptionCode()
                        : quotation == null || quotation.getPaymentOption() == null ? null : quotation.getPaymentOption().getCode()
        );
        List<ContractItemRequest> itemRequests = resolveRequestedItems(request.getItems(), quotation);
        validateItemRequests(itemRequests);

        Map<String, ProductEntity> productsById = loadProducts(itemRequests);
        Map<String, ContractPricingGateway.PricingData> pricingByProductId = contractPricingGateway.resolveBasePrices(
                customer,
                itemRequests.stream().map(ContractItemRequest::getProductId).toList()
        );

        List<PreparedItem> items = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        boolean requiresApproval = false;

        List<ContractInventoryGateway.RequestedInventory> inventoryRequests = itemRequests.stream()
                .map(item -> new ContractInventoryGateway.RequestedInventory(item.getProductId().trim(), normalizeMoney(item.getQuantity())))
                .toList();
        Map<String, ContractInventoryGateway.InventoryAvailability> inventoryAvailability =
                contractInventoryGateway.checkAvailability(inventoryRequests);

        for (ContractItemRequest itemRequest : itemRequests) {
            ProductEntity product = productsById.get(itemRequest.getProductId().trim());
            validateActiveProduct(product);
            ContractPricingGateway.PricingData pricing = pricingByProductId.get(product.getId());
            if (pricing == null || pricing.baseUnitPrice() == null) {
                throw new ContractPricingNotFoundException(product.getId());
            }

            BigDecimal quantity = normalizeMoney(itemRequest.getQuantity());
            BigDecimal baseUnitPrice = normalizeMoney(pricing.baseUnitPrice());
            BigDecimal finalUnitPrice = normalizeMoney(itemRequest.getUnitPrice());
            BigDecimal minimumAllowedPrice = baseUnitPrice.multiply(MINIMUM_ALLOWED_PRICE_FACTOR).setScale(2, RoundingMode.HALF_UP);
            if (finalUnitPrice.compareTo(minimumAllowedPrice) < 0) {
                throw RequestValidationException.singleError("items", "Unit price for product " + product.getProductCode() + " is below allowed minimum");
            }

            BigDecimal priceDeltaPercent = percentageDifference(baseUnitPrice, finalUnitPrice);
            if (finalUnitPrice.compareTo(baseUnitPrice) != 0 || priceDeltaPercent.compareTo(new BigDecimal("10.00")) > 0) {
                requiresApproval = true;
            }
            if (finalUnitPrice.compareTo(baseUnitPrice) < 0) {
                warnings.add("Price override below base price for product " + product.getProductCode() + " requires approval");
            }
            ContractInventoryGateway.InventoryAvailability availability = inventoryAvailability.get(product.getId());
            if (availability != null && !availability.sufficient()) {
                warnings.add("Current stock is insufficient for product " + product.getProductCode());
            }

            BigDecimal discountAmount = baseUnitPrice.subtract(finalUnitPrice).max(BigDecimal.ZERO).multiply(quantity).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalPrice = quantity.multiply(finalUnitPrice).setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(totalPrice).setScale(2, RoundingMode.HALF_UP);

            items.add(new PreparedItem(product, quantity, baseUnitPrice, finalUnitPrice, discountAmount, totalPrice, normalizeNullable(itemRequest.getPriceOverrideReason())));
        }

        if (totalAmount.compareTo(HIGH_VALUE_APPROVAL_THRESHOLD) > 0) {
            requiresApproval = true;
        }

        ContractCreditGateway.CreditSnapshot creditSnapshot = contractCreditGateway.getCreditSnapshot(customer);
        BigDecimal projectedDebt = creditSnapshot.currentDebt().add(totalAmount).setScale(2, RoundingMode.HALF_UP);
        boolean creditExceeded = creditSnapshot.creditLimit().compareTo(BigDecimal.ZERO) > 0
                && projectedDebt.compareTo(creditSnapshot.creditLimit()) > 0;
        if (enforceCreditLimit && creditExceeded) {
            throw new ContractCreditLimitExceededException();
        }
        if (creditExceeded) {
            warnings.add("Projected debt exceeds current credit limit");
        }

        BigDecimal depositPercentage = determineDepositPercentage(customer);
        BigDecimal depositAmount = totalAmount.multiply(depositPercentage).divide(HUNDRED, 2, RoundingMode.HALF_UP);

        return new PreparedContract(
                customer,
                quotation,
                resolvePriceListId(quotation, pricingByProductId),
                items,
                totalAmount,
                requiresApproval,
                requiresApproval ? ContractApprovalTier.OWNER.name() : null,
                creditSnapshot,
                projectedDebt,
                depositPercentage,
                depositAmount,
                warnings,
                StringUtils.hasText(request.getPaymentTerms())
                        ? normalizeNullable(request.getPaymentTerms())
                        : resolveCustomerPaymentTerms(customer),
                paymentOption,
                normalizeNullable(request.getDeliveryAddress()),
                normalizeNullable(request.getDeliveryTerms()),
                normalizeNullable(request.getNote()),
                Boolean.TRUE.equals(request.getConfidential()),
                request.getExpectedDeliveryDate(),
                contractSchedulerSupport.nextAutoSubmitAt(LocalDateTime.now(APP_ZONE))
        );
    }

    private ContractPreviewResponseData toPreviewResponse(PreparedContract prepared) {
        return new ContractPreviewResponseData(
                prepared.customer().getId(),
                prepared.customer().getCompanyName(),
                prepared.quotation() == null ? null : prepared.quotation().getId(),
                prepared.quotation() == null ? null : prepared.quotation().getQuotationNumber(),
                prepared.items().stream()
                        .map(item -> new ContractItemResponse(null, item.product().getId(), item.product().getProductCode(), item.product().getProductName(), item.product().getType(), item.product().getSize(), item.product().getThickness(), item.product().getUnit(), item.quantity(), item.baseUnitPrice(), item.finalUnitPrice(), item.discountAmount(), item.totalPrice(), item.priceOverrideReason()))
                        .toList(),
                contractMapper.toPaymentOptionData(prepared.paymentOption()),
                prepared.totalAmount(),
                prepared.requiresApproval(),
                prepared.approvalTier(),
                prepared.creditSnapshot().creditLimit(),
                prepared.creditSnapshot().currentDebt(),
                prepared.projectedDebt(),
                prepared.depositPercentage(),
                prepared.depositAmount(),
                prepared.warnings(),
                prepared.expectedDeliveryDate(),
                prepared.autoSubmitDueAt()
        );
    }

    private ContractDetailResponseData toDetailResponse(ContractEntity contract) {
        ContractResponse contractResponse = contractMapper.toContractResponse(contract);
        ContractCreditGateway.CreditSnapshot creditSnapshot = contractCreditGateway.getCreditSnapshot(contract.getCustomer());
        BigDecimal projectedDebt = creditSnapshot.currentDebt().add(defaultIfNull(contract.getTotalAmount())).setScale(2, RoundingMode.HALF_UP);
        return new ContractDetailResponseData(
                contractResponse,
                new ContractDetailResponseData.CustomerData(
                        contract.getCustomer() == null ? null : contract.getCustomer().getId(),
                        contract.getCustomer() == null ? null : contract.getCustomer().getCompanyName(),
                        contract.getCustomer() == null ? null : contract.getCustomer().getCustomerType(),
                        contract.getCustomer() == null ? null : contract.getCustomer().getCreditLimit(),
                        contract.getCustomer() == null ? null : contract.getCustomer().getStatus()
                ),
                contract.getQuotation() == null
                        ? null
                        : new ContractDetailResponseData.QuotationData(
                                contract.getQuotation().getId(),
                                contract.getQuotation().getQuotationNumber(),
                                contract.getQuotation().getStatus()
                        ),
                contractMapper.toItemResponses(contract.getItems()),
                new ContractDetailResponseData.ApprovalData(
                        contract.isRequiresApproval(),
                        contract.getApprovalStatus(),
                        contract.getApprovalTier(),
                        contract.getPendingAction(),
                        contract.getApprovalRequestedAt(),
                        contract.getApprovalDueAt(),
                        contract.getApprovedBy(),
                        contract.getApprovedAt()
                ),
                new ContractDetailResponseData.CreditData(
                        creditSnapshot.creditLimit(),
                        creditSnapshot.currentDebt(),
                        projectedDebt,
                        creditSnapshot.creditLimit().subtract(projectedDebt).setScale(2, RoundingMode.HALF_UP)
                ),
                contract.getVersions().stream().map(contractMapper::toVersionData).toList(),
                contract.getStatusHistory().stream().map(contractMapper::toStatusHistoryData).toList(),
                contract.getDocuments().stream().map(contractMapper::toDocumentData).toList(),
                contract.isConfidential() ? "Confidential contract. Customer export/download is restricted by policy." : null
        );
    }

    private void applyPreparedContract(
            ContractEntity contract,
            PreparedContract prepared,
            String operatorId,
            String contractNumber,
            BigDecimal priceChangePercent
    ) {
        contract.setContractNumber(contractNumber);
        contract.setCustomer(prepared.customer());
        contract.setQuotation(prepared.quotation());
        contract.setPriceListId(prepared.priceListId());
        contract.setPaymentTerms(prepared.paymentTerms());
        contract.setPaymentOption(prepared.paymentOption());
        contract.setDeliveryAddress(prepared.deliveryAddress());
        contract.setDeliveryTerms(prepared.deliveryTerms());
        contract.setExpectedDeliveryDate(prepared.expectedDeliveryDate());
        contract.setNote(prepared.note());
        contract.setConfidential(prepared.confidential());
        contract.setTotalAmount(prepared.totalAmount());
        contract.setRequiresApproval(prepared.requiresApproval());
        contract.setApprovalTier(prepared.approvalTier());
        contract.setCreditLimitSnapshot(prepared.creditSnapshot().creditLimit());
        contract.setCurrentDebtSnapshot(prepared.creditSnapshot().currentDebt());
        contract.setDepositPercentage(prepared.depositPercentage());
        contract.setDepositAmount(prepared.depositAmount());
        contract.setPriceChangePercent(normalizeMoney(priceChangePercent));
        contract.setAutoSubmitDueAt(prepared.autoSubmitDueAt());
        contract.setLastTrackingRefreshAt(LocalDateTime.now(APP_ZONE));
        if (!StringUtils.hasText(contract.getCreatedBy())) {
            contract.setCreatedBy(operatorId);
        }
        if (!StringUtils.hasText(contract.getApprovalStatus())) {
            contract.setApprovalStatus(ContractApprovalStatus.NOT_REQUIRED.name());
        }

        contract.getItems().clear();
        for (PreparedItem item : prepared.items()) {
            ContractItemEntity entity = new ContractItemEntity();
            entity.setContract(contract);
            entity.setProduct(item.product());
            entity.setQuantity(item.quantity());
            entity.setBaseUnitPrice(item.baseUnitPrice());
            entity.setUnitPrice(item.finalUnitPrice());
            entity.setDiscountAmount(item.discountAmount());
            entity.setTotalPrice(item.totalPrice());
            entity.setPriceOverrideReason(item.priceOverrideReason());
            contract.getItems().add(entity);
        }
    }

    private ContractPreviewRequest normalizedUpdateRequest(ContractEntity contract, ContractUpdateRequest request) {
        ContractPreviewRequest normalized = new ContractPreviewRequest();
        normalized.setCustomerId(contract.getCustomer().getId());
        normalized.setQuotationId(StringUtils.hasText(request.getQuotationId())
                ? request.getQuotationId()
                : contract.getQuotation() == null ? null : contract.getQuotation().getId());
        normalized.setPaymentTerms(request.getPaymentTerms());
        normalized.setPaymentOptionCode(StringUtils.hasText(request.getPaymentOptionCode())
                ? request.getPaymentOptionCode()
                : contract.getPaymentOption() == null ? null : contract.getPaymentOption().getCode());
        normalized.setDeliveryAddress(request.getDeliveryAddress());
        normalized.setDeliveryTerms(request.getDeliveryTerms());
        normalized.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        normalized.setNote(request.getNote());
        normalized.setConfidential(request.getConfidential());
        normalized.setItems(request.getItems());
        return normalized;
    }

    private CustomerProfileEntity loadCustomer(String customerId) {
        CustomerProfileEntity customer = customerProfileRepository.findById(customerId.trim())
                .orElseThrow(CustomerProfileNotFoundException::new);
        if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
            throw RequestValidationException.singleError("customerId", "Customer must be ACTIVE");
        }
        return customer;
    }

    private QuotationEntity resolveQuotation(String quotationId, String customerId) {
        if (!StringUtils.hasText(quotationId)) {
            return null;
        }
        QuotationEntity quotation = quotationRepository.findDetailedById(quotationId.trim())
                .orElseThrow(QuotationNotFoundException::new);
        validateQuotationForContractUse(quotation, customerId);
        return quotation;
    }

    private void validateQuotationForContractUse(QuotationEntity quotation, String customerId) {
        if (!quotation.getCustomer().getId().equals(customerId)) {
            throw RequestValidationException.singleError("quotationId", "Quotation does not belong to selected customer");
        }
        if (QuotationStatus.CONVERTED.name().equalsIgnoreCase(quotation.getStatus())
                || QuotationStatus.REJECTED.name().equalsIgnoreCase(quotation.getStatus())
                || QuotationStatus.DRAFT.name().equalsIgnoreCase(quotation.getStatus())) {
            throw RequestValidationException.singleError("quotationId", "Quotation is not valid for contract conversion");
        }
        if (quotation.getValidUntil() != null && quotation.getValidUntil().isBefore(LocalDate.now(APP_ZONE))) {
            throw RequestValidationException.singleError("quotationId", "Quotation has expired");
        }
    }

    private List<ContractItemRequest> resolveRequestedItems(List<ContractItemRequest> items, QuotationEntity quotation) {
        if (items != null && !items.isEmpty()) {
            return items;
        }
        if (quotation == null || quotation.getItems() == null || quotation.getItems().isEmpty()) {
            throw RequestValidationException.singleError("items", "At least one contract item is required");
        }

        List<ContractItemRequest> copied = new ArrayList<>();
        for (QuotationItemEntity quotationItem : quotation.getItems()) {
            ContractItemRequest request = new ContractItemRequest();
            request.setProductId(quotationItem.getProduct().getId());
            request.setQuantity(quotationItem.getQuantity());
            request.setUnitPrice(quotationItem.getUnitPrice());
            copied.add(request);
        }
        return copied;
    }

    private void validateItemRequests(List<ContractItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw RequestValidationException.singleError("items", "At least one contract item is required");
        }
        Set<String> uniqueProductIds = new LinkedHashSet<>();
        for (ContractItemRequest item : items) {
            if (!uniqueProductIds.add(item.getProductId().trim())) {
                throw RequestValidationException.singleError("items", "Duplicate productId is not allowed");
            }
        }
    }

    private Map<String, ProductEntity> loadProducts(List<ContractItemRequest> items) {
        List<String> productIds = items.stream().map(item -> item.getProductId().trim()).toList();
        List<ProductEntity> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw RequestValidationException.singleError("items", "One or more products do not exist");
        }
        Map<String, ProductEntity> productMap = new LinkedHashMap<>();
        products.forEach(product -> productMap.put(product.getId(), product));
        return productMap;
    }

    private void validateActiveProduct(ProductEntity product) {
        if (!ProductStatus.ACTIVE.name().equalsIgnoreCase(product.getStatus())) {
            throw RequestValidationException.singleError("items", "Product must be ACTIVE: " + product.getId());
        }
    }

    private BigDecimal determineDepositPercentage(CustomerProfileEntity customer) {
        if (customer != null && customer.getCreatedAt() != null
                && !customer.getCreatedAt().isAfter(LocalDateTime.now(APP_ZONE).minusMonths(6))) {
            return new BigDecimal("20.00");
        }
        return new BigDecimal("30.00");
    }

    private String resolveCustomerPaymentTerms(CustomerProfileEntity customer) {
        if (customer != null && StringUtils.hasText(customer.getPaymentTerms())) {
            return customer.getPaymentTerms().trim();
        }
        return STANDARD_PAYMENT_TERMS;
    }

    private ContractEntity loadAccessibleContract(String contractId, AuthenticatedUser currentUser) {
        if (RoleName.CUSTOMER.name().equalsIgnoreCase(currentUser.role())) {
            CustomerProfileEntity customer = loadCurrentCustomer();
            return contractRepository.findDetailedByIdAndCustomer_Id(contractId, customer.getId())
                    .orElseThrow(ContractNotFoundException::new);
        }
        ensureInternalRole(currentUser);
        return loadDetailedContract(contractId);
    }

    private ContractEntity loadDetailedContract(String contractId) {
        return contractRepository.findDetailedById(contractId).orElseThrow(ContractNotFoundException::new);
    }

    private CustomerProfileEntity loadCurrentCustomer() {
        return customerProfileRepository.findByUser_Id(currentUserProvider.getCurrentUser().userId())
                .orElseThrow(CustomerProfileNotFoundException::new);
    }

    private void ensureInternalRole(AuthenticatedUser currentUser) {
        if (!RoleName.ACCOUNTANT.name().equalsIgnoreCase(currentUser.role())
                && !RoleName.OWNER.name().equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenOperationException("You do not have permission to perform this action");
        }
    }

    private void ensureCustomerRole(AuthenticatedUser currentUser) {
        if (!RoleName.CUSTOMER.name().equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenOperationException("You do not have permission to perform this action");
        }
    }

    private Optional<ContractApprovalEntity> findRelevantApproval(ContractEntity contract) {
        if (contract.getApprovals() == null || contract.getApprovals().isEmpty()) {
            return Optional.empty();
        }
        return contract.getApprovals().stream()
                .filter(approval -> ContractApprovalStatus.PENDING.name().equalsIgnoreCase(approval.getStatus()))
                .findFirst()
                .or(() -> contract.getApprovals().stream().findFirst());
    }

    private BigDecimal percentageDifference(BigDecimal baseline, BigDecimal current) {
        if (baseline == null || baseline.compareTo(BigDecimal.ZERO) == 0 || current == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return current.subtract(baseline).abs().multiply(HUNDRED).divide(baseline, 2, RoundingMode.HALF_UP);
    }

    private List<String> buildApprovalReasons(
            ContractEntity contract,
            ContractApprovalEntity approval,
            ContractCreditGateway.CreditSnapshot creditSnapshot,
            BigDecimal projectedDebt
    ) {
        List<String> reasons = new ArrayList<>();
        if (ContractPendingAction.CANCEL.name().equalsIgnoreCase(contract.getPendingAction())) {
            reasons.add("Cancellation request requires approval before inventory and downstream records are updated");
        }
        if (defaultIfNull(contract.getTotalAmount()).compareTo(HIGH_VALUE_APPROVAL_THRESHOLD) > 0) {
            reasons.add("Contract total exceeds 500,000,000 VND approval threshold");
        }
        if (defaultIfNull(contract.getPriceChangePercent()).compareTo(TEN_PERCENT) > 0) {
            reasons.add("Price change exceeds 10% threshold");
        } else if (contract.isRequiresApproval()) {
            reasons.add("Price override or negotiated terms require approval review");
        }
        if (creditSnapshot.exceeded() || projectedDebt.compareTo(creditSnapshot.creditLimit()) > 0 && creditSnapshot.creditLimit().compareTo(BigDecimal.ZERO) > 0) {
            reasons.add("Projected debt exceeds available customer credit");
        }
        if (contract.isConfidential()) {
            reasons.add("Contract is marked confidential and requires controlled access");
        }
        if (approval != null && StringUtils.hasText(approval.getComment())) {
            reasons.add("Requester note: " + approval.getComment().trim());
        }
        if (reasons.isEmpty()) {
            reasons.add("Owner review required according to approval policy");
        }
        return reasons;
    }

    private Integer customerHistoryMonths(CustomerProfileEntity customer) {
        if (customer == null || customer.getCreatedAt() == null) {
            return null;
        }
        return Math.toIntExact(Math.max(0, ChronoUnit.MONTHS.between(customer.getCreatedAt().toLocalDate(), LocalDate.now(APP_ZONE))));
    }

    private String determineCreditRiskLevel(ContractCreditGateway.CreditSnapshot creditSnapshot, BigDecimal projectedDebt) {
        if (creditSnapshot.creditLimit().compareTo(BigDecimal.ZERO) <= 0) {
            return "UNLIMITED";
        }
        if (projectedDebt.compareTo(creditSnapshot.creditLimit()) > 0) {
            return "HIGH";
        }
        BigDecimal usageRatio = projectedDebt.multiply(HUNDRED).divide(creditSnapshot.creditLimit(), 2, RoundingMode.HALF_UP);
        if (usageRatio.compareTo(new BigDecimal("80.00")) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String determineApprovalActionRecommendation(ContractEntity contract) {
        if (ContractPendingAction.CANCEL.name().equalsIgnoreCase(contract.getPendingAction())) {
            return "Review inventory release, invoicing impact, and customer communication before deciding.";
        }
        return "Review credit exposure, negotiated pricing, and delivery/payment terms before deciding.";
    }

    private ContractApprovalResponseData decideApproval(
            String contractId,
            ContractApprovalDecisionRequest request,
            ContractApprovalStatus decision,
            String decisionLabel
    ) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (!RoleName.OWNER.name().equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenOperationException("You do not have permission to perform this action");
        }

        ContractEntity contract = loadDetailedContract(contractId);
        ContractApprovalEntity approval = loadPendingApproval(contract);
        approval.setStatus(decision.name());
        approval.setComment(normalizeNullable(request.getComment()));
        approval.setDecidedBy(currentUser.userId());
        approval.setDecidedAt(LocalDateTime.now(APP_ZONE));
        contractApprovalRepository.save(approval);

        String previousStatus = contract.getStatus();
        contract.setApprovalStatus(decision.name());
        contract.setPendingAction(null);
        contract.setApprovedBy(currentUser.userId());
        contract.setApprovedAt(LocalDateTime.now(APP_ZONE));
        contract.setApprovalRequestedAt(null);
        contract.setApprovalDueAt(null);

        if (ContractApprovalStatus.APPROVED == decision) {
            if (ContractPendingAction.CANCEL.name().equalsIgnoreCase(resolvePendingAction(contract, approval))) {
                finalizeCancellation(
                        contract,
                        resolveCancellationReasonCode(contract, approval),
                        contract.getCancellationNote(),
                        currentUser.userId()
                );
                recordTrackingEvent(contract, ContractTrackingEventType.CANCELLED, ContractStatus.CANCELLED.name(), "Cancellation approved", normalizeNullable(request.getComment()), null, null, currentUser.userId());
                contractNotificationGateway.notifyCancellation(contract, "Cancellation approved");
            } else {
                assertInventoryAvailable(contract);
                prepareSubmittedContract(contract, currentUser.userId());
                contractInventoryGateway.reserveInventory(contract);
                recordTrackingEvent(contract, ContractTrackingEventType.APPROVED, ContractStatus.SUBMITTED.name(), "Approval granted", normalizeNullable(request.getComment()), null, null, currentUser.userId());
                recordTrackingEvent(contract, ContractTrackingEventType.INVENTORY_RESERVED, ContractStatus.SUBMITTED.name(), "Inventory reserved", "Reservation requested for fulfillment", null, null, currentUser.userId());
                contractNotificationGateway.notifyContractApproved(contract, "Contract approved and submitted");
                contractNotificationGateway.notifyWarehousePreparation(contract, "Contract approved and submitted");
            }
        } else if (ContractApprovalStatus.REJECTED == decision) {
            if (!ContractPendingAction.CANCEL.name().equalsIgnoreCase(resolvePendingAction(contract, approval))) {
                contract.setStatus(ContractStatus.DRAFT.name());
            }
            recordTrackingEvent(contract, ContractTrackingEventType.REJECTED, contract.getStatus(), "Approval rejected", normalizeNullable(request.getComment()), null, null, currentUser.userId());
            contractNotificationGateway.notifyApprovalDecision(contract, decision.name(), "Approval rejected");
        } else {
            contract.setStatus(ContractStatus.DRAFT.name());
            recordTrackingEvent(contract, ContractTrackingEventType.REJECTED, contract.getStatus(), "Modification requested", normalizeNullable(request.getComment()), null, null, currentUser.userId());
            contractNotificationGateway.notifyApprovalDecision(contract, decision.name(), "Modification requested");
        }

        contract.setLastStatusChangeAt(LocalDateTime.now(APP_ZONE));
        ContractEntity saved = contractRepository.save(contract);
        if (!saved.getStatus().equals(previousStatus) || ContractApprovalStatus.APPROVED == decision) {
            recordStatusHistory(saved, previousStatus, saved.getStatus(), normalizeNullable(request.getComment()), currentUser.userId());
        }
        logAudit("DECIDE_CONTRACT_APPROVAL", saved.getId(), previousStatus, saved.getStatus(), currentUser.userId());
        return new ContractApprovalResponseData(saved.getId(), saved.getContractNumber(), saved.getApprovalStatus(), saved.getStatus(), decisionLabel, currentUser.userId(), approval.getDecidedAt(), normalizeNullable(request.getComment()));
    }

    private String resolvePendingAction(ContractEntity contract, ContractApprovalEntity approval) {
        if (StringUtils.hasText(contract.getPendingAction())) {
            return contract.getPendingAction();
        }
        return ContractApprovalType.CANCELLATION.name().equalsIgnoreCase(approval.getApprovalType())
                ? ContractPendingAction.CANCEL.name()
                : ContractPendingAction.SUBMIT.name();
    }

    private ContractApprovalResponseData moveBackToDraft(
            ContractEntity contract,
            String decision,
            ContractTrackingEventType trackingEventType,
            String trackingTitle,
            String comment,
            String actorUserId,
            String auditAction
    ) {
        String previousStatus = contract.getStatus();
        contract.setStatus(ContractStatus.DRAFT.name());
        contract.setApprovalStatus(ContractApprovalStatus.NOT_REQUIRED.name());
        contract.setPendingAction(null);
        contract.setApprovalRequestedAt(null);
        contract.setApprovalDueAt(null);
        contract.setUpdatedBy(actorUserId);
        contract.setLastStatusChangeAt(LocalDateTime.now(APP_ZONE));
        ContractEntity saved = contractRepository.save(contract);

        recordStatusHistory(saved, previousStatus, ContractStatus.DRAFT.name(), comment, actorUserId);
        recordTrackingEvent(saved, trackingEventType, ContractStatus.DRAFT.name(), trackingTitle, comment, null, null, actorUserId);
        contractNotificationGateway.notifyApprovalDecision(saved, decision, trackingTitle);
        logAudit(auditAction, saved.getId(), previousStatus, saved.getStatus(), actorUserId);
        return new ContractApprovalResponseData(
                saved.getId(),
                saved.getContractNumber(),
                saved.getApprovalStatus(),
                saved.getStatus(),
                decision,
                actorUserId,
                saved.getLastStatusChangeAt(),
                comment
        );
    }

    private void validateContractReadyForSubmission(ContractEntity contract) {
        if (!StringUtils.hasText(contract.getPaymentTerms())) {
            throw new ContractSubmitNotAllowedException("Payment terms are required before submission");
        }
        if (!StringUtils.hasText(contract.getDeliveryAddress())) {
            throw new ContractSubmitNotAllowedException("Delivery address is required before submission");
        }
        if (contract.getItems() == null || contract.getItems().isEmpty()) {
            throw new ContractSubmitNotAllowedException("At least one contract item is required before submission");
        }
        if (!StringUtils.hasText(contract.getCustomer().getStatus()) || !"ACTIVE".equalsIgnoreCase(contract.getCustomer().getStatus())) {
            throw new ContractSubmitNotAllowedException("Customer must be ACTIVE before submission");
        }
        if (ContractApprovalStatus.PENDING.name().equalsIgnoreCase(contract.getApprovalStatus())) {
            throw new ContractSubmitNotAllowedException("Contract already has a pending approval");
        }
    }

    private void enforceCreditLimit(ContractEntity contract) {
        ContractCreditGateway.CreditSnapshot creditSnapshot = contractCreditGateway.getCreditSnapshot(contract.getCustomer());
        if (creditSnapshot.creditLimit().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal projectedDebt = creditSnapshot.currentDebt().add(defaultIfNull(contract.getTotalAmount())).setScale(2, RoundingMode.HALF_UP);
        if (projectedDebt.compareTo(creditSnapshot.creditLimit()) > 0) {
            throw new ContractCreditLimitExceededException();
        }
    }

    private void assertInventoryAvailable(ContractEntity contract) {
        List<ContractInventoryGateway.RequestedInventory> requests = contract.getItems().stream()
                .map(item -> new ContractInventoryGateway.RequestedInventory(item.getProduct().getId(), item.getQuantity()))
                .toList();
        Map<String, ContractInventoryGateway.InventoryAvailability> availability = contractInventoryGateway.checkAvailability(requests);
        for (ContractItemEntity item : contract.getItems()) {
            ContractInventoryGateway.InventoryAvailability itemAvailability = availability.get(item.getProduct().getId());
            if (itemAvailability != null && !itemAvailability.sufficient()) {
                throw new ContractInventoryUnavailableException("Insufficient inventory for product " + item.getProduct().getProductCode());
            }
        }
    }

    private void ensureCancellable(ContractEntity contract) {
        if (ContractStatus.CANCELLED.name().equalsIgnoreCase(contract.getStatus())
                || ContractStatus.COMPLETED.name().equalsIgnoreCase(contract.getStatus())
                || ContractStatus.DELIVERED.name().equalsIgnoreCase(contract.getStatus())) {
            throw new ContractCancelNotAllowedException("Completed, delivered, or cancelled contract cannot be cancelled");
        }
        if (ContractApprovalStatus.PENDING.name().equalsIgnoreCase(contract.getApprovalStatus())) {
            throw new ContractCancelNotAllowedException("Contract already has a pending approval");
        }
    }

    private ContractEntity finalizeCancellation(
            ContractEntity contract,
            String cancellationReasonCode,
            String cancellationNote,
            String actingUserId
    ) {
        contractInventoryGateway.releaseReservation(contract, cancellationReasonCode);
        applyCancellationDebtSettlement(contract, cancellationReasonCode, actingUserId);
        contract.setStatus(ContractStatus.CANCELLED.name());
        contract.setCancelledBy(actingUserId);
        contract.setCancelledAt(LocalDateTime.now(APP_ZONE));
        contract.setCancellationReasonCode(cancellationReasonCode);
        contract.setCancellationNote(cancellationNote);
        contract.setPendingAction(null);
        contract.setLastStatusChangeAt(LocalDateTime.now(APP_ZONE));
        contract.setUpdatedBy(actingUserId);
        return contractRepository.save(contract);
    }

    private void applyCancellationDebtSettlement(ContractEntity contract, String cancellationReasonCode, String actingUserId) {
        CancellationDebtRule rule = determineCancellationDebtRule(contract, cancellationReasonCode);
        List<InvoiceEntity> contractInvoices = invoiceRepository.findByContractIdWithCustomerAndContract(contract.getId());
        List<String> invoiceIds = contractInvoices.stream().map(InvoiceEntity::getId).toList();
        List<PaymentAllocationEntity> allocations = invoiceIds.isEmpty()
                ? List.of()
                : paymentAllocationRepository.findDetailedByInvoiceIds(invoiceIds);

        if (rule.receivableAmount().compareTo(BigDecimal.ZERO) <= 0) {
            cancelContractInvoices(contractInvoices, actingUserId, rule.note());
            return;
        }

        InvoiceEntity settlementInvoice = resolveSettlementInvoice(contract, contractInvoices, actingUserId, rule.note());
        configureSettlementInvoice(settlementInvoice, contract, rule, actingUserId);
        InvoiceEntity savedSettlementInvoice = invoiceRepository.save(settlementInvoice);

        if (!allocations.isEmpty()) {
            DebtInvoiceEntity settlementDebtInvoice = debtInvoiceRepository.getReferenceById(savedSettlementInvoice.getId());
            for (PaymentAllocationEntity allocation : allocations) {
                allocation.setInvoice(settlementDebtInvoice);
            }
            paymentAllocationRepository.saveAll(allocations);
        }

        cancelNonSettlementInvoices(contractInvoices, savedSettlementInvoice.getId(), actingUserId, rule.note());
    }

    private void cancelContractInvoices(List<InvoiceEntity> contractInvoices, String actingUserId, String cancellationReason) {
        if (contractInvoices.isEmpty()) {
            return;
        }
        LocalDateTime cancelledAt = LocalDateTime.now(APP_ZONE);
        for (InvoiceEntity invoice : contractInvoices) {
            invoice.setStatus("CANCELLED");
            invoice.setCancellationReason(cancellationReason);
            invoice.setCancelledBy(actingUserId);
            invoice.setCancelledAt(cancelledAt);
            invoice.setUpdatedBy(actingUserId);
        }
        invoiceRepository.saveAll(contractInvoices);
    }

    private void cancelNonSettlementInvoices(
            List<InvoiceEntity> contractInvoices,
            String settlementInvoiceId,
            String actingUserId,
            String cancellationReason
    ) {
        if (contractInvoices.isEmpty()) {
            return;
        }
        List<InvoiceEntity> staleInvoices = contractInvoices.stream()
                .filter(invoice -> !invoice.getId().equals(settlementInvoiceId))
                .toList();
        cancelContractInvoices(staleInvoices, actingUserId, cancellationReason);
    }

    private InvoiceEntity resolveSettlementInvoice(
            ContractEntity contract,
            List<InvoiceEntity> contractInvoices,
            String actingUserId,
            String note
    ) {
        Optional<InvoiceEntity> existing = contractInvoices.stream()
                .filter(invoice -> !"CANCELLED".equalsIgnoreCase(invoice.getStatus()) && !"VOID".equalsIgnoreCase(invoice.getStatus()))
                .findFirst();
        if (existing.isPresent()) {
            return invoiceRepository.findDetailedById(existing.get().getId()).orElse(existing.get());
        }

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber(buildCancellationInvoiceNumber(contract));
        invoice.setContract(contract);
        invoice.setCustomer(contract.getCustomer());
        invoice.setSourceType("CONTRACT_CANCELLATION");
        invoice.setCustomerName(contract.getCustomer() == null ? null : normalizeNullable(contract.getCustomer().getCompanyName()));
        invoice.setCustomerTaxCode(contract.getCustomer() == null ? null : normalizeNullable(contract.getCustomer().getTaxCode()));
        invoice.setBillingAddress(contract.getCustomer() == null ? null : normalizeNullable(contract.getCustomer().getAddress()));
        invoice.setIssueDate(LocalDate.now(APP_ZONE));
        invoice.setDueDate(LocalDate.now(APP_ZONE).plusDays(7));
        invoice.setPaymentTerms("Cancellation settlement");
        invoice.setCreatedBy(actingUserId);
        invoice.setUpdatedBy(actingUserId);
        invoice.setNote(note);
        return invoice;
    }

    private void configureSettlementInvoice(
            InvoiceEntity invoice,
            ContractEntity contract,
            CancellationDebtRule rule,
            String actingUserId
    ) {
        invoice.setContract(contract);
        invoice.setCustomer(contract.getCustomer());
        invoice.setSourceType("CONTRACT_CANCELLATION");
        invoice.setCustomerName(contract.getCustomer() == null ? null : normalizeNullable(contract.getCustomer().getCompanyName()));
        invoice.setCustomerTaxCode(contract.getCustomer() == null ? null : normalizeNullable(contract.getCustomer().getTaxCode()));
        invoice.setBillingAddress(contract.getCustomer() == null ? null : normalizeNullable(contract.getCustomer().getAddress()));
        if (invoice.getIssueDate() == null) {
            invoice.setIssueDate(LocalDate.now(APP_ZONE));
        }
        if (invoice.getDueDate() == null || invoice.getDueDate().isBefore(invoice.getIssueDate())) {
            invoice.setDueDate(invoice.getIssueDate().plusDays(7));
        }
        invoice.setPaymentTerms("Cancellation settlement");
        invoice.setAdjustmentAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        invoice.setTotalAmount(rule.receivableAmount());
        invoice.setVatRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        invoice.setVatAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        invoice.setStatus("ISSUED");
        invoice.setCancellationReason(null);
        invoice.setCancelledBy(null);
        invoice.setCancelledAt(null);
        invoice.setUpdatedBy(actingUserId);
        invoice.setNote(rule.note());
        replaceSettlementInvoiceItems(invoice, rule);
    }

    private void replaceSettlementInvoiceItems(InvoiceEntity invoice, CancellationDebtRule rule) {
        invoice.getItems().clear();
        InvoiceItemEntity item = new InvoiceItemEntity();
        item.setInvoice(invoice);
        item.setDescription(rule.description());
        item.setUnit("CASE");
        item.setQuantity(BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP));
        item.setUnitPrice(rule.receivableAmount());
        item.setTotalPrice(rule.receivableAmount());
        invoice.getItems().add(item);
    }

    private CancellationDebtRule determineCancellationDebtRule(ContractEntity contract, String cancellationReasonCode) {
        if (!isCustomerCancellation(cancellationReasonCode)) {
            return new CancellationDebtRule(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    "Company-initiated cancellation clears receivable exposure; refund and compensation are handled outside receivable debt.",
                    "Contract cancellation settlement cleared"
            );
        }

        String normalizedStatus = normalizeUpper(contract.getStatus());
        if (CUSTOMER_LOADING_STATUSES.contains(normalizedStatus)) {
            BigDecimal depositHoldback = resolveCancellationDepositHoldback(contract);
            return new CancellationDebtRule(
                    depositHoldback,
                    "Customer cancellation after goods were loaded keeps the contract deposit; any two-way transport charge must be added manually.",
                    "Customer cancellation charge after loading"
            );
        }

        LocalDateTime referenceTime = contract.getSubmittedAt() != null ? contract.getSubmittedAt() : contract.getCreatedAt();
        boolean withinTwentyFourHours = referenceTime != null
                && !referenceTime.plusHours(24).isBefore(LocalDateTime.now(APP_ZONE))
                && !PREPARATION_STARTED_STATUSES.contains(normalizedStatus);
        if (withinTwentyFourHours) {
            return new CancellationDebtRule(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    "Customer cancellation within 24 hours before warehouse confirmation returns the deposit in full, so no receivable remains.",
                    "Customer cancellation within 24 hours"
            );
        }

        BigDecimal chargeAmount = defaultIfNull(contract.getTotalAmount())
                .multiply(TEN_PERCENT)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return new CancellationDebtRule(
                chargeAmount,
                "Customer cancellation after 24 hours keeps 10% of the contract value as a cancellation receivable.",
                "Customer cancellation charge 10% of contract value"
        );
    }

    private BigDecimal resolveCancellationDepositHoldback(ContractEntity contract) {
        BigDecimal depositAmount = defaultIfNull(contract.getDepositAmount());
        if (depositAmount.compareTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)) > 0) {
            return depositAmount;
        }
        return defaultIfNull(contract.getTotalAmount())
                .multiply(THIRTY_PERCENT)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private boolean isCustomerCancellation(String cancellationReasonCode) {
        return ContractCancellationReason.CUSTOMER_REQUEST.name().equalsIgnoreCase(normalizeUpper(cancellationReasonCode));
    }

    private String resolveCancellationReasonCode(ContractEntity contract, ContractApprovalEntity approval) {
        if (StringUtils.hasText(contract.getCancellationReasonCode())) {
            return contract.getCancellationReasonCode().trim();
        }
        return approval == null ? ContractCancellationReason.OTHER.name() : normalizeUpper(approval.getComment());
    }

    private String buildCancellationInvoiceNumber(ContractEntity contract) {
        String compactId = contract.getId() == null ? "UNKNOWN" : contract.getId().replace("-", "").toUpperCase();
        return "INV-CXL-" + compactId.substring(0, Math.min(8, compactId.length()));
    }

    private boolean requiresCancellationApproval(ContractEntity contract) {
        return defaultIfNull(contract.getTotalAmount()).compareTo(CANCELLATION_APPROVAL_THRESHOLD) > 0;
    }

    private void createApprovalRequest(
            ContractEntity contract,
            ContractApprovalType approvalType,
            ContractApprovalTier approvalTier,
            ContractPendingAction pendingAction,
            String comment,
            String requestedBy
    ) {
        contract.setStatus(ContractStatus.PENDING_APPROVAL.name());
        contract.setApprovalStatus(ContractApprovalStatus.PENDING.name());
        contract.setApprovalTier(approvalTier.name());
        contract.setPendingAction(pendingAction.name());
        contract.setApprovalRequestedAt(LocalDateTime.now(APP_ZONE));
        contract.setApprovalDueAt(contractSchedulerSupport.approvalDueAt(contract.getApprovalRequestedAt()));
        contract.setLastStatusChangeAt(LocalDateTime.now(APP_ZONE));
        contractRepository.save(contract);

        ContractApprovalEntity approval = new ContractApprovalEntity();
        approval.setContract(contract);
        approval.setApprovalType(approvalType.name());
        approval.setApprovalTier(approvalTier.name());
        approval.setStatus(ContractApprovalStatus.PENDING.name());
        approval.setRequestedBy(requestedBy);
        approval.setRequestedAt(contract.getApprovalRequestedAt());
        approval.setDueAt(contract.getApprovalDueAt());
        approval.setComment(comment);
        contractApprovalRepository.save(approval);
    }

    private ContractApprovalEntity loadPendingApproval(ContractEntity contract) {
        return contractApprovalRepository.findByContract_IdOrderByRequestedAtDesc(contract.getId()).stream()
                .filter(approval -> ContractApprovalStatus.PENDING.name().equalsIgnoreCase(approval.getStatus()))
                .findFirst()
                .orElseThrow(ContractApprovalNotFoundException::new);
    }

    private ContractDocumentEntity loadDocument(ContractEntity contract, String documentId) {
        return contract.getDocuments().stream()
                .filter(document -> document.getId().equals(documentId))
                .findFirst()
                .orElseThrow(ContractDocumentNotFoundException::new);
    }

    private void ensureCustomerDocumentAccess(ContractEntity contract, AuthenticatedUser currentUser, boolean generating) {
        if (generating) {
            ensureInternalRole(currentUser);
            return;
        }
        if (RoleName.CUSTOMER.name().equalsIgnoreCase(currentUser.role()) && contract.isConfidential()) {
            throw new ForbiddenOperationException("Confidential contract documents require stronger internal access");
        }
    }

    private void markQuotationConverted(QuotationEntity quotation, ContractEntity contract, String userId) {
        String previousStatus = quotation.getStatus();
        quotation.setStatus(QuotationStatus.CONVERTED.name());
        quotationRepository.save(quotation);
        logQuotationAudit(quotation.getId(), previousStatus, contract.getId(), userId);
    }

    private void recordVersion(ContractEntity contract, String changeReason, String userId) {
        ContractVersionEntity version = new ContractVersionEntity();
        version.setContract(contract);
        version.setVersionNo((int) contractVersionRepository.countByContract_Id(contract.getId()) + 1);
        version.setChangeReason(changeReason);
        version.setSnapshot(toJson(snapshotPayload(contract)));
        version.setChangedBy(userId);
        contractVersionRepository.save(version);
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
        event.setTrackingNumber(trackingNumber);
        event.setCreatedBy(createdBy);
        contractTrackingEventRepository.save(event);
    }

    private long nextDocumentSequence(ContractDocumentType documentType) {
        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return contractDocumentRepository.countByDocumentTypeAndOfficialDocumentTrueAndGeneratedAtBetween(documentType.name(), start, end) + 1;
    }

    private void prepareSubmittedContract(ContractEntity contract, String userId) {
        LocalDateTime submittedAt = contract.getSubmittedAt() == null ? LocalDateTime.now(APP_ZONE) : contract.getSubmittedAt();
        contract.setStatus(ContractStatus.SUBMITTED.name());
        contract.setSubmittedBy(userId);
        contract.setSubmittedAt(submittedAt);
        contract.setPendingAction(null);
        contract.setLastStatusChangeAt(LocalDateTime.now(APP_ZONE));
        if (!StringUtils.hasText(contract.getSaleOrderNumber())) {
            contract.setSaleOrderNumber(generateSaleOrderNumber(submittedAt));
        }
        if (contract.getItems() == null) {
            return;
        }
        for (ContractItemEntity item : contract.getItems()) {
            if (item == null) {
                continue;
            }
            item.setReservedQuantity(defaultQuantity(item.getQuantity()));
            if (item.getIssuedQuantity() == null) {
                item.setIssuedQuantity(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }
            if (item.getDeliveredQuantity() == null) {
                item.setDeliveredQuantity(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }
        }
    }

    private BigDecimal computePriceChangePercent(ContractEntity contract, List<ContractItemRequest> updatedItems) {
        if (updatedItems == null || updatedItems.isEmpty() || contract.getItems() == null || contract.getItems().isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        Map<String, BigDecimal> oldPriceByProductId = new LinkedHashMap<>();
        contract.getItems().forEach(item -> oldPriceByProductId.put(item.getProduct().getId(), normalizeMoney(item.getUnitPrice())));

        BigDecimal maxChange = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (ContractItemRequest item : updatedItems) {
            BigDecimal oldPrice = oldPriceByProductId.get(item.getProductId().trim());
            if (oldPrice == null || oldPrice.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal change = percentageDifference(oldPrice, normalizeMoney(item.getUnitPrice()));
            if (change.compareTo(maxChange) > 0) {
                maxChange = change;
            }
        }
        return maxChange;
    }

    private String generateContractNumber() {
        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        long sequence = contractRepository.countByCreatedAtBetween(startOfDay, endOfDay) + 1;
        return "CT-" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%04d", sequence);
    }

    private String generateSaleOrderNumber(LocalDateTime submittedAt) {
        LocalDate effectiveDate = submittedAt == null ? LocalDate.now(APP_ZONE) : submittedAt.toLocalDate();
        LocalDateTime startOfDay = effectiveDate.atStartOfDay();
        LocalDateTime endOfDay = effectiveDate.plusDays(1).atStartOfDay();
        long sequence = contractRepository.countBySubmittedAtBetween(startOfDay, endOfDay) + 1;
        return "SO-" + effectiveDate.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%04d", sequence);
    }

    private Sort buildContractSort(ContractListQuery query) {
        String sortBy = StringUtils.hasText(query.getSortBy()) && CONTRACT_SORT_FIELDS.contains(query.getSortBy())
                ? query.getSortBy()
                : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sortBy);
    }

    private Sort buildPendingApprovalSort(PendingContractApprovalListQuery query) {
        String sortBy = StringUtils.hasText(query.getSortBy()) && PENDING_APPROVAL_SORT_FIELDS.contains(query.getSortBy())
                ? query.getSortBy()
                : "approvalRequestedAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sortBy);
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 20 : pageSize;
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : normalizeMoney(value);
    }

    private BigDecimal defaultQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolvePriceListId(
            QuotationEntity quotation,
            Map<String, ContractPricingGateway.PricingData> pricingByProductId
    ) {
        return pricingByProductId.values().stream()
                .map(ContractPricingGateway.PricingData::priceListId)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(quotation == null ? null : quotation.getPriceListId());
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : "";
    }

    private List<PaymentOptionData> loadAvailablePaymentOptions() {
        return paymentOptionRepository.findByActiveTrueOrderByDisplayOrderAscCodeAsc().stream()
                .map(contractMapper::toPaymentOptionData)
                .toList();
    }

    private PaymentOptionEntity resolvePaymentOption(String paymentOptionCode) {
        if (!StringUtils.hasText(paymentOptionCode)) {
            return null;
        }
        return paymentOptionRepository.findByCodeIgnoreCaseAndActiveTrue(paymentOptionCode.trim())
                .orElseThrow(() -> RequestValidationException.singleError("paymentOptionCode", "Payment option is invalid or inactive"));
    }

    private Map<String, Object> snapshotPayload(ContractEntity contract) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contractNumber", contract.getContractNumber());
        payload.put("customerId", contract.getCustomer() == null ? null : contract.getCustomer().getId());
        payload.put("status", contract.getStatus());
        payload.put("approvalStatus", contract.getApprovalStatus());
        payload.put("totalAmount", contract.getTotalAmount());
        payload.put("paymentTerms", contract.getPaymentTerms());
        payload.put("paymentOptionCode", contract.getPaymentOption() == null ? null : contract.getPaymentOption().getCode());
        payload.put("deliveryAddress", contract.getDeliveryAddress());
        payload.put("items", contract.getItems().stream()
                .map(item -> Map.of(
                        "productId", item.getProduct().getId(),
                        "quantity", item.getQuantity(),
                        "baseUnitPrice", item.getBaseUnitPrice(),
                        "unitPrice", item.getUnitPrice(),
                        "totalPrice", item.getTotalPrice()
                ))
                .toList());
        return payload;
    }

    private void logAudit(String action, String entityId, Object oldValue, Object newValue, String userId) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType("CONTRACT");
        auditLog.setEntityId(entityId);
        auditLog.setOldValue(toJson(oldValue));
        auditLog.setNewValue(toJson(newValue));
        auditLogRepository.save(auditLog);
    }

    private void logQuotationAudit(String quotationId, Object oldValue, Object newValue, String userId) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(userId);
        auditLog.setAction("CONVERT_QUOTATION_TO_CONTRACT");
        auditLog.setEntityType("QUOTATION");
        auditLog.setEntityId(quotationId);
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

    private record PreparedContract(
            CustomerProfileEntity customer,
            QuotationEntity quotation,
            String priceListId,
            List<PreparedItem> items,
            BigDecimal totalAmount,
            boolean requiresApproval,
            String approvalTier,
            ContractCreditGateway.CreditSnapshot creditSnapshot,
            BigDecimal projectedDebt,
            BigDecimal depositPercentage,
            BigDecimal depositAmount,
            List<String> warnings,
            String paymentTerms,
            PaymentOptionEntity paymentOption,
            String deliveryAddress,
            String deliveryTerms,
            String note,
            boolean confidential,
            LocalDate expectedDeliveryDate,
            LocalDateTime autoSubmitDueAt
    ) {
    }

    private record PreparedItem(
            ProductEntity product,
            BigDecimal quantity,
            BigDecimal baseUnitPrice,
            BigDecimal finalUnitPrice,
            BigDecimal discountAmount,
            BigDecimal totalPrice,
            String priceOverrideReason
    ) {
    }

    private record CancellationDebtRule(
            BigDecimal receivableAmount,
            String note,
            String description
    ) {
    }
}
