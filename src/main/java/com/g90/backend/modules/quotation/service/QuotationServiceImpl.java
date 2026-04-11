package com.g90.backend.modules.quotation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.CustomerProfileNotFoundException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.PromotionNotApplicableException;
import com.g90.backend.exception.QuotationAmountTooLowException;
import com.g90.backend.exception.QuotationNotEditableException;
import com.g90.backend.exception.QuotationNotFoundException;
import com.g90.backend.exception.QuotationNotSubmittableException;
import com.g90.backend.exception.QuotationPricingNotFoundException;
import com.g90.backend.exception.QuotationProjectAccessException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.contract.repository.ContractRepository;
import com.g90.backend.modules.customer.entity.CustomerPriceGroup;
import com.g90.backend.modules.payment.dto.PaymentOptionData;
import com.g90.backend.modules.payment.entity.PaymentOptionEntity;
import com.g90.backend.modules.payment.repository.PaymentOptionRepository;
import com.g90.backend.modules.pricing.entity.PriceListEntity;
import com.g90.backend.modules.pricing.entity.PriceListItemEntity;
import com.g90.backend.modules.pricing.repository.PriceListRepository;
import com.g90.backend.modules.product.dto.PaginationResponse;
import com.g90.backend.modules.product.dto.ProductListQuery;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.entity.ProductStatus;
import com.g90.backend.modules.product.repository.ProductRepository;
import com.g90.backend.modules.product.repository.ProductSpecifications;
import com.g90.backend.modules.promotion.entity.PromotionEntity;
import com.g90.backend.modules.promotion.entity.PromotionProductEntity;
import com.g90.backend.modules.promotion.repository.PromotionRepository;
import com.g90.backend.modules.quotation.dto.CustomerQuotationListQuery;
import com.g90.backend.modules.quotation.dto.CustomerQuotationListResponseData;
import com.g90.backend.modules.quotation.dto.CustomerQuotationSummaryResponseData;
import com.g90.backend.modules.quotation.dto.QuotationDetailResponseData;
import com.g90.backend.modules.quotation.dto.QuotationFormInitQuery;
import com.g90.backend.modules.quotation.dto.QuotationFormInitResponseData;
import com.g90.backend.modules.quotation.dto.QuotationHistoryResponseData;
import com.g90.backend.modules.quotation.dto.QuotationItemRequest;
import com.g90.backend.modules.quotation.dto.QuotationItemResponse;
import com.g90.backend.modules.quotation.dto.QuotationManagementListQuery;
import com.g90.backend.modules.quotation.dto.QuotationManagementListResponseData;
import com.g90.backend.modules.quotation.dto.QuotationPreviewByIdResponseData;
import com.g90.backend.modules.quotation.dto.QuotationPreviewResponseData;
import com.g90.backend.modules.quotation.dto.QuotationSaveResponseData;
import com.g90.backend.modules.quotation.dto.QuotationSubmitActionRequest;
import com.g90.backend.modules.quotation.dto.QuotationSubmitRequest;
import com.g90.backend.modules.quotation.dto.QuotationSubmitResponseData;
import com.g90.backend.modules.quotation.entity.ProjectEntity;
import com.g90.backend.modules.quotation.entity.QuotationEntity;
import com.g90.backend.modules.quotation.entity.QuotationItemEntity;
import com.g90.backend.modules.quotation.entity.QuotationStatus;
import com.g90.backend.modules.quotation.mapper.QuotationMapper;
import com.g90.backend.modules.quotation.repository.ProjectRepository;
import com.g90.backend.modules.quotation.repository.QuotationRepository;
import com.g90.backend.modules.quotation.repository.QuotationSpecifications;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class QuotationServiceImpl implements QuotationService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal MIN_TOTAL_AMOUNT = new BigDecimal("10000000.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final Set<String> BLOCKED_PROJECT_STATUSES = Set.of("INACTIVE", "LOCKED", "CLOSED");
    private static final Set<String> CUSTOMER_QUOTATION_SORT_FIELDS = Set.of(
            "createdAt",
            "quotationNumber",
            "totalAmount",
            "validUntil",
            "status"
    );
    private static final Set<String> MANAGEMENT_QUOTATION_SORT_FIELDS = Set.of(
            "createdAt",
            "quotationNumber",
            "totalAmount",
            "validUntil",
            "status"
    );

    private final QuotationRepository quotationRepository;
    private final ProjectRepository projectRepository;
    private final ProductRepository productRepository;
    private final PriceListRepository priceListRepository;
    private final PromotionRepository promotionRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogRepository auditLogRepository;
    private final ContractRepository contractRepository;
    private final PaymentOptionRepository paymentOptionRepository;
    private final QuotationMapper quotationMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public QuotationFormInitResponseData getQuotationFormInit(QuotationFormInitQuery query) {
        CustomerProfileEntity customer = loadCurrentCustomer();
        ResolvedPricing resolvedPricing = resolvePricing(customer);
        Map<String, BigDecimal> unitPriceByProductId = resolvedPricing.unitPriceByProductId();

        ProductListQuery productQuery = new ProductListQuery();
        productQuery.setKeyword(normalizeNullable(query.getKeyword()));
        productQuery.setType(normalizeNullable(query.getType()));
        productQuery.setSize(normalizeNullable(query.getSize()));
        productQuery.setThickness(normalizeNullable(query.getThickness()));
        productQuery.setStatus(ProductStatus.ACTIVE.name());

        Page<ProductEntity> products = productRepository.findAll(
                ProductSpecifications.withFilters(productQuery),
                PageRequest.of(normalizePage(query.getPage()) - 1, normalizePageSize(query.getPageSize()), Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<QuotationFormInitResponseData.ProductData> productData = products.stream()
                .map(product -> new QuotationFormInitResponseData.ProductData(
                        product.getId(),
                        product.getProductCode(),
                        product.getProductName(),
                        product.getType(),
                        product.getSize(),
                        product.getThickness(),
                        product.getUnit(),
                        product.getReferenceWeight(),
                        product.getStatus(),
                        unitPriceByProductId.get(product.getId())
                ))
                .toList();

        List<QuotationFormInitResponseData.ProjectData> projects = projectRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId()).stream()
                .filter(this::isProjectSelectable)
                .map(project -> new QuotationFormInitResponseData.ProjectData(
                        project.getId(),
                        project.getProjectCode(),
                        project.getName(),
                        project.getStatus()
                ))
                .toList();

        List<QuotationFormInitResponseData.PromotionData> promotions = promotionRepository.findVisibleActivePromotions(
                        LocalDate.now(APP_ZONE),
                        resolveCustomerPromotionGroups(customer)
                ).stream()
                .map(promotion -> new QuotationFormInitResponseData.PromotionData(
                        promotion.getCode(),
                        promotion.getName(),
                        promotion.getPromotionType(),
                        normalizeMoney(promotion.getDiscountValue())
                ))
                .toList();

        return new QuotationFormInitResponseData(
                new QuotationFormInitResponseData.CustomerData(
                        customer.getId(),
                        customer.getCompanyName(),
                        customer.getCustomerType(),
                        customer.getStatus()
                ),
                productData,
                projects,
                promotions,
                loadAvailablePaymentOptions()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationPreviewResponseData previewQuotation(QuotationSubmitRequest request) {
        CustomerProfileEntity customer = loadCurrentCustomer();
        PreparedQuotation preparedQuotation = prepareQuotation(customer, request, true, false);
        return new QuotationPreviewResponseData(
                preparedQuotation.project() == null
                        ? null
                        : new QuotationPreviewResponseData.ProjectData(
                                preparedQuotation.project().getId(),
                                preparedQuotation.project().getProjectCode(),
                                preparedQuotation.project().getName()
                        ),
                preparedQuotation.itemResponses(),
                new QuotationPreviewResponseData.SummaryData(
                        preparedQuotation.subTotal(),
                        preparedQuotation.discountAmount(),
                        preparedQuotation.totalAmount()
                ),
                preparedQuotation.promotion() == null
                        ? null
                        : new QuotationPreviewResponseData.PromotionData(
                                preparedQuotation.promotion().code(),
                                preparedQuotation.promotion().name(),
                                preparedQuotation.promotion().applied()
                        ),
                preparedQuotation.deliveryRequirements(),
                preparedQuotation.validUntil(),
                new QuotationPreviewResponseData.ValidationData(true, List.of())
        );
    }

    @Override
    @Transactional
    public QuotationSubmitResponseData createQuotation(QuotationSubmitRequest request) {
        CustomerProfileEntity customer = loadCurrentCustomer();
        PreparedQuotation preparedQuotation = prepareQuotation(customer, request, true, true);

        QuotationEntity quotation = new QuotationEntity();
        applyPreparedQuotation(
                quotation,
                customer,
                preparedQuotation.project(),
                preparedQuotation,
                QuotationStatus.PENDING,
                generateQuotationNumber(),
                LocalDateTime.now(APP_ZONE)
        );

        QuotationEntity savedQuotation = quotationRepository.save(quotation);
        QuotationSubmitResponseData response = quotationMapper.toSubmitResponse(savedQuotation, "Waiting for accountant review");
        logAudit("CREATE_QUOTATION", savedQuotation.getId(), null, response, customer.getUser().getId());
        return response;
    }

    @Override
    @Transactional
    public QuotationSaveResponseData saveDraftQuotation(QuotationSubmitRequest request) {
        CustomerProfileEntity customer = loadCurrentCustomer();
        PreparedQuotation preparedQuotation = prepareQuotation(customer, request, false, false);

        QuotationEntity quotation = new QuotationEntity();
        applyPreparedQuotation(
                quotation,
                customer,
                preparedQuotation.project(),
                preparedQuotation,
                QuotationStatus.DRAFT,
                generateQuotationNumber(),
                null
        );

        QuotationEntity savedQuotation = quotationRepository.save(quotation);
        QuotationSaveResponseData response = quotationMapper.toSaveResponse(savedQuotation);
        logAudit("SAVE_QUOTATION_DRAFT", savedQuotation.getId(), null, response, customer.getUser().getId());
        return response;
    }

    @Override
    @Transactional
    public QuotationSubmitResponseData submitQuotation(QuotationSubmitActionRequest request) {
        if (StringUtils.hasText(request.getQuotationId())) {
            return submitQuotation(request.getQuotationId());
        }

        validateSubmitActionRequest(request);
        QuotationSubmitRequest submitRequest = new QuotationSubmitRequest();
        submitRequest.setProjectId(request.getProjectId());
        submitRequest.setDeliveryRequirements(request.getDeliveryRequirements());
        submitRequest.setPromotionCode(request.getPromotionCode());
        submitRequest.setPaymentOptionCode(request.getPaymentOptionCode());
        submitRequest.setNote(request.getNote());
        submitRequest.setItems(request.getItems());
        return createQuotation(submitRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationPreviewByIdResponseData getQuotationPreview(String quotationId) {
        QuotationEntity quotation = loadAccessibleQuotation(quotationId, currentUserProvider.getCurrentUser());
        PersistedQuotationSummary summary = calculatePersistedSummary(quotation);
        PromotionEntity promotion = findActivePromotion(quotation.getPromotionCode());

        return new QuotationPreviewByIdResponseData(
                new QuotationPreviewByIdResponseData.QuotationData(
                        quotation.getId(),
                        quotation.getQuotationNumber(),
                        quotation.getStatus(),
                        quotation.getCreatedAt(),
                        quotation.getValidUntil(),
                        quotation.getProject() == null
                                ? null
                                : new QuotationPreviewByIdResponseData.ProjectData(
                                        quotation.getProject().getId(),
                                        quotation.getProject().getProjectCode(),
                                        quotation.getProject().getName()
                                ),
                        quotation.getDeliveryRequirement(),
                        !StringUtils.hasText(quotation.getPromotionCode())
                                ? null
                                : new QuotationPreviewByIdResponseData.PromotionData(
                                        quotation.getPromotionCode(),
                                        promotion == null ? null : promotion.getName()
                                ),
                        quotationMapper.toPaymentOptionData(quotation.getPaymentOption())
                ),
                quotationMapper.toItemResponses(quotation.getItems()),
                new QuotationPreviewResponseData.SummaryData(
                        summary.subTotal(),
                        summary.discountAmount(),
                        summary.totalAmount()
                )
        );
    }

    @Override
    @Transactional
    public QuotationSaveResponseData updateDraftQuotation(String quotationId, QuotationSubmitRequest request) {
        CustomerProfileEntity customer = loadCurrentCustomer();
        QuotationEntity quotation = loadOwnedQuotation(quotationId);
        ensureDraft(quotation, true);

        QuotationSaveResponseData oldState = quotationMapper.toSaveResponse(quotation);
        PreparedQuotation preparedQuotation = prepareQuotation(customer, request, false, false);
        applyPreparedQuotation(
                quotation,
                customer,
                preparedQuotation.project(),
                preparedQuotation,
                QuotationStatus.DRAFT,
                StringUtils.hasText(quotation.getQuotationNumber()) ? quotation.getQuotationNumber() : generateQuotationNumber(),
                null
        );

        QuotationEntity savedQuotation = quotationRepository.save(quotation);
        QuotationSaveResponseData response = quotationMapper.toSaveResponse(savedQuotation);
        logAudit("UPDATE_QUOTATION", savedQuotation.getId(), oldState, response, customer.getUser().getId());
        return response;
    }

    @Override
    @Transactional
    public QuotationSubmitResponseData submitQuotation(String quotationId) {
        CustomerProfileEntity customer = loadCurrentCustomer();
        QuotationEntity quotation = loadOwnedQuotation(quotationId);
        ensureDraft(quotation, false);
        if (quotation.getTotalAmount() == null || quotation.getTotalAmount().compareTo(MIN_TOTAL_AMOUNT) < 0) {
            throw new QuotationAmountTooLowException();
        }

        QuotationSubmitResponseData oldState = quotationMapper.toSubmitResponse(quotation, null);
        quotation.setStatus(QuotationStatus.PENDING.name());
        quotation.setSubmittedAt(LocalDateTime.now(APP_ZONE));
        if (!StringUtils.hasText(quotation.getQuotationNumber())) {
            quotation.setQuotationNumber(generateQuotationNumber());
        }

        QuotationEntity savedQuotation = quotationRepository.save(quotation);
        QuotationSubmitResponseData response = quotationMapper.toSubmitResponse(savedQuotation, "Waiting for accountant review");
        logAudit("SUBMIT_QUOTATION", savedQuotation.getId(), oldState, response, customer.getUser().getId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerQuotationListResponseData getMyQuotations(CustomerQuotationListQuery query) {
        CustomerProfileEntity customer = loadCurrentCustomer();
        Page<QuotationEntity> quotations = quotationRepository.findAll(
                QuotationSpecifications.forCustomer(customer.getId(), query),
                PageRequest.of(normalizePage(query.getPage()) - 1, normalizePageSize(query.getPageSize()), buildCustomerQuotationSort(query))
        );

        List<CustomerQuotationListResponseData.ItemData> items = quotations.stream()
                .map(quotation -> new CustomerQuotationListResponseData.ItemData(
                        quotation.getId(),
                        quotation.getQuotationNumber(),
                        quotation.getCreatedAt(),
                        quotation.getTotalAmount(),
                        quotation.getStatus(),
                        quotation.getValidUntil(),
                        new CustomerQuotationListResponseData.ActionData(true, isDraft(quotation), !isDraft(quotation))
                ))
                .toList();

        return new CustomerQuotationListResponseData(
                items,
                PaginationResponse.builder()
                        .page(quotations.getNumber() + 1)
                        .pageSize(quotations.getSize())
                        .totalItems(quotations.getTotalElements())
                        .totalPages(quotations.getTotalPages())
                        .build(),
                new CustomerQuotationListResponseData.FilterData(
                        normalizeNullable(query.getStatus()),
                        query.getFromDate(),
                        query.getToDate()
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerQuotationSummaryResponseData getMyQuotationSummary() {
        CustomerProfileEntity customer = loadCurrentCustomer();
        return new CustomerQuotationSummaryResponseData(
                quotationRepository.countByCustomer_Id(customer.getId()),
                quotationRepository.countByCustomer_IdAndStatusIgnoreCase(customer.getId(), QuotationStatus.DRAFT.name()),
                quotationRepository.countByCustomer_IdAndStatusIgnoreCase(customer.getId(), QuotationStatus.PENDING.name()),
                quotationRepository.countByCustomer_IdAndStatusIgnoreCase(customer.getId(), QuotationStatus.CONVERTED.name()),
                quotationRepository.countByCustomer_IdAndStatusIgnoreCase(customer.getId(), QuotationStatus.REJECTED.name())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationManagementListResponseData getQuotations(QuotationManagementListQuery query) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        String scopedCustomerId = null;
        if (RoleName.CUSTOMER.name().equalsIgnoreCase(currentUser.role())) {
            scopedCustomerId = loadCurrentCustomer().getId();
        } else if (!hasAnyRole(currentUser.role(), RoleName.ACCOUNTANT, RoleName.OWNER)) {
            throw new ForbiddenOperationException("You do not have permission to perform this action");
        }

        Page<QuotationEntity> quotations = quotationRepository.findAll(
                QuotationSpecifications.byQuery(query, scopedCustomerId),
                PageRequest.of(normalizePage(query.getPage()) - 1, normalizePageSize(query.getPageSize()), buildManagementQuotationSort(query))
        );

        List<QuotationManagementListResponseData.Item> items = quotations.stream()
                .map(quotation -> new QuotationManagementListResponseData.Item(
                        quotation.getId(),
                        quotation.getQuotationNumber(),
                        quotation.getCustomer() == null ? null : quotation.getCustomer().getId(),
                        quotation.getCustomer() == null ? null : quotation.getCustomer().getCompanyName(),
                        quotation.getTotalAmount(),
                        resolveQuotationDisplayStatus(quotation),
                        quotation.getValidUntil(),
                        quotation.getCreatedAt(),
                        RoleName.CUSTOMER.name().equalsIgnoreCase(currentUser.role()) && isDraft(quotation),
                        hasAnyRole(currentUser.role(), RoleName.ACCOUNTANT, RoleName.OWNER) && canCreateContract(quotation)
                ))
                .toList();

        return new QuotationManagementListResponseData(
                items,
                PaginationResponse.builder()
                        .page(quotations.getNumber() + 1)
                        .pageSize(quotations.getSize())
                        .totalItems(quotations.getTotalElements())
                        .totalPages(quotations.getTotalPages())
                        .build(),
                new QuotationManagementListResponseData.Filters(
                        normalizeNullable(query.getQuotationNumber()),
                        normalizeNullable(scopedCustomerId != null ? scopedCustomerId : query.getCustomerId()),
                        normalizeNullable(query.getStatus()),
                        query.getFromDate(),
                        query.getToDate()
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationDetailResponseData getQuotationDetail(String quotationId) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        QuotationEntity quotation = loadAccessibleQuotation(quotationId, currentUser);
        PersistedQuotationSummary summary = calculatePersistedSummary(quotation);

        boolean customerCanEdit = RoleName.CUSTOMER.name().equalsIgnoreCase(currentUser.role()) && isDraft(quotation);
        boolean accountantCanCreateContract = hasAnyRole(currentUser.role(), RoleName.ACCOUNTANT, RoleName.OWNER)
                && canCreateContract(quotation);

        return new QuotationDetailResponseData(
                new QuotationDetailResponseData.QuotationData(
                        quotation.getId(),
                        quotation.getQuotationNumber(),
                        quotation.getStatus(),
                        quotation.getTotalAmount(),
                        quotation.getValidUntil(),
                        quotation.getCreatedAt()
                ),
                new QuotationDetailResponseData.CustomerData(
                        quotation.getCustomer().getId(),
                        quotation.getCustomer().getCompanyName(),
                        quotation.getCustomer().getTaxCode(),
                        quotation.getCustomer().getAddress(),
                        quotation.getCustomer().getContactPerson(),
                        quotation.getCustomer().getPhone(),
                        quotation.getCustomer().getEmail(),
                        quotation.getCustomer().getCustomerType()
                ),
                quotation.getProject() == null
                        ? null
                        : new QuotationDetailResponseData.ProjectData(
                                quotation.getProject().getId(),
                                quotation.getProject().getProjectCode(),
                                quotation.getProject().getName(),
                                quotation.getProject().getLocation(),
                                quotation.getProject().getStatus()
                        ),
                quotationMapper.toItemResponses(quotation.getItems()),
                new QuotationDetailResponseData.PricingData(
                        summary.subTotal(),
                        summary.discountAmount(),
                        summary.totalAmount(),
                        quotation.getPromotionCode()
                ),
                quotationMapper.toPaymentOptionData(quotation.getPaymentOption()),
                quotation.getDeliveryRequirement(),
                new QuotationDetailResponseData.ActionData(customerCanEdit, accountantCanCreateContract)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationHistoryResponseData getQuotationHistory(String quotationId) {
        loadAccessibleQuotation(quotationId, currentUserProvider.getCurrentUser());
        List<AuditLogEntity> logs = auditLogRepository.findQuotationHistory(quotationId);

        Set<String> userIds = logs.stream()
                .map(AuditLogEntity::getUserId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, UserAccountEntity> usersById = loadUsersById(userIds);
        Map<String, CustomerProfileEntity> customersByUserId = loadCustomersByUserId(userIds);

        List<QuotationHistoryResponseData.EventData> events = logs.stream()
                .map(log -> {
                    UserAccountEntity user = usersById.get(log.getUserId());
                    CustomerProfileEntity customer = customersByUserId.get(log.getUserId());
                    return new QuotationHistoryResponseData.EventData(
                            log.getId(),
                            mapHistoryAction(log.getAction()),
                            user == null || user.getRole() == null ? "SYSTEM" : user.getRole().getName(),
                            resolveActorName(user, customer),
                            mapHistoryNote(log.getAction()),
                            log.getCreatedAt()
                    );
                })
                .toList();

        return new QuotationHistoryResponseData(quotationId, events);
    }

    private CustomerProfileEntity loadCurrentCustomer() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (!RoleName.CUSTOMER.name().equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenOperationException("You do not have permission to perform this action");
        }

        UserAccountEntity user = userAccountRepository.findWithRoleById(currentUser.userId())
                .orElseThrow(CustomerProfileNotFoundException::new);
        CustomerProfileEntity customer = customerProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(CustomerProfileNotFoundException::new);
        if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
            throw RequestValidationException.singleError("customer", "Customer profile must be ACTIVE");
        }
        return customer;
    }

    private QuotationEntity loadOwnedQuotation(String quotationId) {
        CustomerProfileEntity customer = loadCurrentCustomer();
        return quotationRepository.findDetailedByIdAndCustomer_Id(quotationId, customer.getId())
                .orElseThrow(QuotationNotFoundException::new);
    }

    private QuotationEntity loadAccessibleQuotation(String quotationId, AuthenticatedUser currentUser) {
        if (RoleName.CUSTOMER.name().equalsIgnoreCase(currentUser.role())) {
            CustomerProfileEntity customer = loadCurrentCustomer();
            return quotationRepository.findDetailedByIdAndCustomer_Id(quotationId, customer.getId())
                    .orElseThrow(QuotationNotFoundException::new);
        }
        if (!hasAnyRole(currentUser.role(), RoleName.ACCOUNTANT, RoleName.OWNER)) {
            throw new ForbiddenOperationException("You do not have permission to perform this action");
        }
        return quotationRepository.findDetailedById(quotationId)
                .orElseThrow(QuotationNotFoundException::new);
    }

    private PreparedQuotation prepareQuotation(
            CustomerProfileEntity customer,
            QuotationSubmitRequest request,
            boolean pricingRequired,
            boolean enforceMinimumAmount
    ) {
        validateQuotationItems(request.getItems());
        validateDuplicateProducts(request.getItems());

        ProjectEntity project = resolveProject(customer.getId(), normalizeNullable(request.getProjectId()));

        Map<String, ProductEntity> products = loadProducts(request.getItems());
        ResolvedPricing resolvedPricing = resolvePricing(customer);
        Map<String, BigDecimal> unitPriceByProductId = resolvedPricing.unitPriceByProductId();

        List<PreparedQuotationItem> preparedItems = new ArrayList<>();
        List<QuotationItemResponse> itemResponses = new ArrayList<>();
        BigDecimal subTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (QuotationItemRequest itemRequest : request.getItems()) {
            ProductEntity product = products.get(itemRequest.getProductId().trim());
            validateProduct(product);

            BigDecimal quantity = normalizeMoney(itemRequest.getQuantity());
            BigDecimal unitPrice = resolveUnitPrice(itemRequest.getProductId().trim(), unitPriceByProductId, pricingRequired);
            BigDecimal totalPrice = unitPrice == null
                    ? null
                    : quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            if (totalPrice != null) {
                subTotal = subTotal.add(totalPrice).setScale(2, RoundingMode.HALF_UP);
            }

            preparedItems.add(new PreparedQuotationItem(product, quantity, unitPrice, totalPrice));
            itemResponses.add(quotationMapper.toItemResponse(null, product, quantity, unitPrice, totalPrice));
        }

        PromotionOutcome promotion = resolvePromotion(customer, normalizeNullable(request.getPromotionCode()), preparedItems, subTotal);
        BigDecimal totalAmount = subTotal.subtract(promotion.discountAmount()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        if (enforceMinimumAmount && totalAmount.compareTo(MIN_TOTAL_AMOUNT) < 0) {
            throw new QuotationAmountTooLowException();
        }
        PaymentOptionEntity paymentOption = resolvePaymentOption(request.getPaymentOptionCode());

        return new PreparedQuotation(
                resolvedPricing.priceListId(),
                project,
                LocalDate.now(APP_ZONE).plusDays(15),
                subTotal,
                promotion.discountAmount(),
                totalAmount,
                preparedItems,
                itemResponses,
                normalizeNullable(request.getNote()),
                normalizeNullable(request.getDeliveryRequirements()),
                promotion,
                paymentOption
        );
    }

    private void validateSubmitActionRequest(QuotationSubmitActionRequest request) {
        validateQuotationItems(request.getItems());
    }

    private void validateQuotationItems(List<QuotationItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw RequestValidationException.singleError("items", "At least one quotation item is required");
        }
        if (items.size() > 20) {
            throw RequestValidationException.singleError("items", "Quotation can contain at most 20 items");
        }
        for (int index = 0; index < items.size(); index++) {
            QuotationItemRequest item = items.get(index);
            if (item != null && !item.getUnexpectedFields().isEmpty()) {
                String unsupportedField = item.getUnexpectedFields().iterator().next();
                throw RequestValidationException.singleError(
                        "items[" + index + "]." + unsupportedField,
                        "Field is not allowed in quotation item request"
                );
            }
        }
    }

    private void validateDuplicateProducts(List<QuotationItemRequest> items) {
        Set<String> uniqueProductIds = new LinkedHashSet<>();
        for (QuotationItemRequest item : items) {
            String productId = item.getProductId().trim();
            if (!uniqueProductIds.add(productId)) {
                throw RequestValidationException.singleError("items", "Duplicate productId is not allowed");
            }
        }
    }

    private ProjectEntity resolveProject(String customerId, String projectId) {
        if (!StringUtils.hasText(projectId)) {
            return null;
        }
        ProjectEntity project = projectRepository.findByIdAndCustomer_Id(projectId, customerId)
                .orElseThrow(QuotationProjectAccessException::new);

        if (!isProjectSelectable(project)) {
            throw new QuotationProjectAccessException();
        }
        return project;
    }

    private Map<String, ProductEntity> loadProducts(List<QuotationItemRequest> items) {
        List<String> productIds = items.stream()
                .map(item -> item.getProductId().trim())
                .toList();

        List<ProductEntity> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw RequestValidationException.singleError("productId", "One or more products do not exist");
        }

        Map<String, ProductEntity> productMap = new LinkedHashMap<>();
        products.forEach(product -> productMap.put(product.getId(), product));
        return productMap;
    }

    private List<PriceListEntity> resolveApplicablePriceLists(CustomerProfileEntity customer) {
        String customerGroup = normalizeNullable(customer.getPriceGroup());
        if (!StringUtils.hasText(customerGroup)) {
            customerGroup = normalizeNullable(customer.getCustomerType());
        }
        if (!StringUtils.hasText(customerGroup)) {
            return List.of();
        }

        return priceListRepository.findApplicablePriceLists(customerGroup, LocalDate.now(APP_ZONE));
    }

    private ResolvedPricing resolvePricing(CustomerProfileEntity customer) {
        List<PriceListEntity> applicablePriceLists = resolveApplicablePriceLists(customer);
        Map<String, BigDecimal> unitPriceByProductId = new LinkedHashMap<>();
        if (applicablePriceLists.isEmpty()) {
            return new ResolvedPricing(null, unitPriceByProductId);
        }

        PriceListEntity selectedPriceList = applicablePriceLists.get(0);
        for (PriceListItemEntity item : selectedPriceList.getItems()) {
            if (item.getDeletedAt() == null && item.getProduct() != null && item.getUnitPrice() != null) {
                unitPriceByProductId.putIfAbsent(item.getProduct().getId(), normalizeMoney(item.getUnitPrice()));
            }
        }
        return new ResolvedPricing(selectedPriceList.getId(), unitPriceByProductId);
    }

    private BigDecimal resolveUnitPrice(
            String productId,
            Map<String, BigDecimal> unitPriceByProductId,
            boolean pricingRequired
    ) {
        BigDecimal resolved = unitPriceByProductId.get(productId);
        if (resolved != null) {
            return resolved;
        }
        if (pricingRequired) {
            throw new QuotationPricingNotFoundException(productId);
        }
        return null;
    }

    private PromotionOutcome resolvePromotion(
            CustomerProfileEntity customer,
            String promotionCode,
            List<PreparedQuotationItem> items,
            BigDecimal subTotal
    ) {
        if (!StringUtils.hasText(promotionCode)) {
            return new PromotionOutcome(null, null, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), false);
        }

        PromotionEntity promotion = promotionRepository.findApplicableByCode(promotionCode, LocalDate.now(APP_ZONE))
                .orElseThrow(() -> new PromotionNotApplicableException(promotionCode));

        Set<String> selectedProductIds = items.stream()
                .map(item -> item.product().getId())
                .collect(Collectors.toSet());
        if (!isPromotionApplicableToCustomer(promotion, resolveCustomerPromotionGroups(customer))) {
            throw new PromotionNotApplicableException(promotionCode);
        }
        if (!isPromotionApplicableToProducts(promotion, selectedProductIds)) {
            throw new PromotionNotApplicableException(promotionCode);
        }

        BigDecimal discountAmount = calculateDiscountAmount(promotion, subTotal);
        return new PromotionOutcome(promotion.getCode(), promotion.getName(), discountAmount, discountAmount.compareTo(BigDecimal.ZERO) > 0);
    }

    private boolean isPromotionApplicableToProducts(PromotionEntity promotion, Set<String> selectedProductIds) {
        if (promotion.getProducts() == null || promotion.getProducts().isEmpty()) {
            return true;
        }
        return promotion.getProducts().stream()
                .filter(scope -> scope.getDeletedAt() == null)
                .map(PromotionProductEntity::getProduct)
                .filter(product -> product != null)
                .map(ProductEntity::getId)
                .anyMatch(selectedProductIds::contains);
    }

    private boolean isPromotionApplicableToCustomer(PromotionEntity promotion, Set<String> customerGroups) {
        if (promotion.getCustomerGroups() == null || promotion.getCustomerGroups().isEmpty()) {
            return true;
        }
        return promotion.getCustomerGroups().stream()
                .filter(scope -> scope.getDeletedAt() == null)
                .map(scope -> scope.getCustomerGroup() == null ? null : scope.getCustomerGroup().trim().toUpperCase(Locale.ROOT))
                .filter(Objects::nonNull)
                .anyMatch(customerGroups::contains);
    }

    private Set<String> resolveCustomerPromotionGroups(CustomerProfileEntity customer) {
        Set<String> groups = new LinkedHashSet<>();
        groups.add(CustomerPriceGroup.DEFAULT);
        if (customer == null) {
            return groups;
        }
        if (StringUtils.hasText(customer.getPriceGroup())) {
            groups.add(customer.getPriceGroup().trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(customer.getCustomerType())) {
            groups.add(customer.getCustomerType().trim().toUpperCase(Locale.ROOT));
        }
        return groups;
    }

    private BigDecimal calculateDiscountAmount(PromotionEntity promotion, BigDecimal subTotal) {
        if (promotion.getDiscountValue() == null || subTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal rawDiscount;
        if ("PERCENT".equalsIgnoreCase(promotion.getPromotionType())) {
            rawDiscount = subTotal.multiply(promotion.getDiscountValue())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        } else {
            rawDiscount = normalizeMoney(promotion.getDiscountValue());
        }
        return rawDiscount.min(subTotal).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateProduct(ProductEntity product) {
        if (!ProductStatus.ACTIVE.name().equalsIgnoreCase(product.getStatus())) {
            throw RequestValidationException.singleError("productId", "Product must be ACTIVE: " + product.getId());
        }
    }

    private void applyPreparedQuotation(
            QuotationEntity quotation,
            CustomerProfileEntity customer,
            ProjectEntity project,
            PreparedQuotation preparedQuotation,
            QuotationStatus status,
            String quotationNumber,
            LocalDateTime submittedAt
    ) {
        quotation.setCustomer(customer);
        quotation.setProject(project);
        quotation.setPriceListId(preparedQuotation.priceListId());
        quotation.setQuotationNumber(quotationNumber);
        quotation.setStatus(status.name());
        quotation.setValidUntil(preparedQuotation.validUntil());
        quotation.setTotalAmount(preparedQuotation.totalAmount());
        quotation.setNote(preparedQuotation.note());
        quotation.setDeliveryRequirement(preparedQuotation.deliveryRequirements());
        quotation.setPromotionCode(preparedQuotation.promotion() == null ? null : preparedQuotation.promotion().code());
        quotation.setPaymentOption(preparedQuotation.paymentOption());
        quotation.setSubmittedAt(submittedAt);

        quotation.getItems().clear();
        for (PreparedQuotationItem preparedItem : preparedQuotation.items()) {
            QuotationItemEntity itemEntity = new QuotationItemEntity();
            itemEntity.setQuotation(quotation);
            itemEntity.setProduct(preparedItem.product());
            itemEntity.setQuantity(preparedItem.quantity());
            itemEntity.setUnitPrice(preparedItem.unitPrice());
            itemEntity.setTotalPrice(preparedItem.totalPrice());
            quotation.getItems().add(itemEntity);
        }
    }

    private PersistedQuotationSummary calculatePersistedSummary(QuotationEntity quotation) {
        BigDecimal subTotal = quotation.getItems().stream()
                .map(item -> item.getTotalPrice() != null
                        ? item.getTotalPrice()
                        : defaultIfNull(item.getUnitPrice()).multiply(defaultIfNull(item.getQuantity())).setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = quotation.getTotalAmount() == null ? subTotal : normalizeMoney(quotation.getTotalAmount());
        BigDecimal discountAmount = subTotal.subtract(totalAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new PersistedQuotationSummary(subTotal, discountAmount, totalAmount);
    }

    private PromotionEntity findActivePromotion(String promotionCode) {
        if (!StringUtils.hasText(promotionCode)) {
            return null;
        }
        return promotionRepository.findApplicableByCode(promotionCode, LocalDate.now(APP_ZONE)).orElse(null);
    }

    private Map<String, UserAccountEntity> loadUsersById(Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userAccountRepository.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(UserAccountEntity::getId, user -> user));
    }

    private Map<String, CustomerProfileEntity> loadCustomersByUserId(Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return customerProfileRepository.findByUser_IdIn(userIds).stream()
                .filter(customer -> customer.getUser() != null)
                .collect(Collectors.toMap(customer -> customer.getUser().getId(), customer -> customer));
    }

    private String resolveActorName(UserAccountEntity user, CustomerProfileEntity customer) {
        if (customer != null && StringUtils.hasText(customer.getCompanyName())) {
            return customer.getCompanyName();
        }
        if (user != null && StringUtils.hasText(user.getFullName())) {
            return user.getFullName();
        }
        if (user != null && StringUtils.hasText(user.getEmail())) {
            return user.getEmail();
        }
        return "System";
    }

    private String mapHistoryAction(String action) {
        return switch (action) {
            case "SAVE_QUOTATION_DRAFT" -> "CREATED";
            case "UPDATE_QUOTATION" -> "UPDATED";
            case "CREATE_QUOTATION", "SUBMIT_QUOTATION" -> "SUBMITTED";
            case "CREATE_CONTRACT_FROM_QUOTATION" -> "CONVERTED";
            default -> action;
        };
    }

    private String mapHistoryNote(String action) {
        return switch (action) {
            case "SAVE_QUOTATION_DRAFT" -> "Quotation draft created";
            case "UPDATE_QUOTATION" -> "Quotation updated";
            case "CREATE_QUOTATION", "SUBMIT_QUOTATION" -> "Quotation submitted for review";
            case "CREATE_CONTRACT_FROM_QUOTATION" -> "Contract created from quotation";
            default -> action.replace('_', ' ').toLowerCase();
        };
    }

    private boolean isProjectSelectable(ProjectEntity project) {
        return !StringUtils.hasText(project.getStatus())
                || !BLOCKED_PROJECT_STATUSES.contains(project.getStatus().trim().toUpperCase());
    }

    private void ensureDraft(QuotationEntity quotation, boolean editableAction) {
        if (isDraft(quotation)) {
            return;
        }
        if (editableAction) {
            throw new QuotationNotEditableException();
        }
        throw new QuotationNotSubmittableException();
    }

    private boolean isDraft(QuotationEntity quotation) {
        return QuotationStatus.DRAFT.name().equalsIgnoreCase(quotation.getStatus());
    }

    private boolean canCreateContract(QuotationEntity quotation) {
        if (contractRepository.existsByQuotation_Id(quotation.getId())) {
            return false;
        }
        if (QuotationStatus.CONVERTED.name().equalsIgnoreCase(quotation.getStatus())
                || QuotationStatus.REJECTED.name().equalsIgnoreCase(quotation.getStatus())
                || QuotationStatus.DRAFT.name().equalsIgnoreCase(quotation.getStatus())) {
            return false;
        }
        return quotation.getTotalAmount() != null && quotation.getTotalAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean hasAnyRole(String currentRole, RoleName... allowedRoles) {
        for (RoleName role : allowedRoles) {
            if (role.name().equalsIgnoreCase(currentRole)) {
                return true;
            }
        }
        return false;
    }

    private Sort buildCustomerQuotationSort(CustomerQuotationListQuery query) {
        String requestedSortBy = normalizeNullable(query.getSortBy());
        String sortBy = StringUtils.hasText(requestedSortBy) && CUSTOMER_QUOTATION_SORT_FIELDS.contains(requestedSortBy)
                ? requestedSortBy
                : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sortBy);
    }

    private Sort buildManagementQuotationSort(QuotationManagementListQuery query) {
        String requestedSortBy = normalizeNullable(query.getSortBy());
        String sortBy = StringUtils.hasText(requestedSortBy) && MANAGEMENT_QUOTATION_SORT_FIELDS.contains(requestedSortBy)
                ? requestedSortBy
                : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sortBy);
    }

    private String resolveQuotationDisplayStatus(QuotationEntity quotation) {
        if (quotation.getValidUntil() != null
                && quotation.getValidUntil().isBefore(LocalDate.now(APP_ZONE))
                && !QuotationStatus.CONVERTED.name().equalsIgnoreCase(quotation.getStatus())
                && !QuotationStatus.REJECTED.name().equalsIgnoreCase(quotation.getStatus())) {
            return "EXPIRED";
        }
        return quotation.getStatus();
    }

    private String generateQuotationNumber() {
        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        long sequence = quotationRepository.countByCreatedAtBetween(startOfDay, endOfDay) + 1;
        return "QT-" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%04d", sequence);
    }

    private void logAudit(String action, String entityId, Object oldValue, Object newValue, String userId) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType("QUOTATION");
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

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : normalizeMoney(value);
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 20 : pageSize;
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private List<PaymentOptionData> loadAvailablePaymentOptions() {
        return paymentOptionRepository.findByActiveTrueOrderByDisplayOrderAscCodeAsc().stream()
                .map(quotationMapper::toPaymentOptionData)
                .toList();
    }

    private PaymentOptionEntity resolvePaymentOption(String paymentOptionCode) {
        if (!StringUtils.hasText(paymentOptionCode)) {
            return null;
        }
        return paymentOptionRepository.findByCodeIgnoreCaseAndActiveTrue(paymentOptionCode.trim())
                .orElseThrow(() -> RequestValidationException.singleError("paymentOptionCode", "Payment option is invalid or inactive"));
    }

    private record PreparedQuotation(
            String priceListId,
            ProjectEntity project,
            LocalDate validUntil,
            BigDecimal subTotal,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            List<PreparedQuotationItem> items,
            List<QuotationItemResponse> itemResponses,
            String note,
            String deliveryRequirements,
            PromotionOutcome promotion,
            PaymentOptionEntity paymentOption
    ) {
    }

    private record ResolvedPricing(
            String priceListId,
            Map<String, BigDecimal> unitPriceByProductId
    ) {
    }

    private record PreparedQuotationItem(
            ProductEntity product,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {
    }

    private record PromotionOutcome(
            String code,
            String name,
            BigDecimal discountAmount,
            boolean applied
    ) {
    }

    private record PersistedQuotationSummary(
            BigDecimal subTotal,
            BigDecimal discountAmount,
            BigDecimal totalAmount
    ) {
    }
}
