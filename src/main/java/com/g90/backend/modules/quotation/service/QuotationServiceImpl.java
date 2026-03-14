package com.g90.backend.modules.quotation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.CustomerProfileNotFoundException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.QuotationAmountTooLowException;
import com.g90.backend.exception.QuotationPricingNotFoundException;
import com.g90.backend.exception.QuotationProjectAccessException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.pricing.entity.PriceListEntity;
import com.g90.backend.modules.pricing.entity.PriceListItemEntity;
import com.g90.backend.modules.pricing.repository.PriceListRepository;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.entity.ProductStatus;
import com.g90.backend.modules.product.repository.ProductRepository;
import com.g90.backend.modules.quotation.dto.QuotationItemResponse;
import com.g90.backend.modules.quotation.dto.QuotationPreviewResponseData;
import com.g90.backend.modules.quotation.dto.QuotationResponseData;
import com.g90.backend.modules.quotation.dto.QuotationSubmitRequest;
import com.g90.backend.modules.quotation.entity.ProjectEntity;
import com.g90.backend.modules.quotation.entity.QuotationEntity;
import com.g90.backend.modules.quotation.entity.QuotationItemEntity;
import com.g90.backend.modules.quotation.entity.QuotationStatus;
import com.g90.backend.modules.quotation.mapper.QuotationMapper;
import com.g90.backend.modules.quotation.repository.ProjectRepository;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class QuotationServiceImpl implements QuotationService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal MIN_TOTAL_AMOUNT = new BigDecimal("10000000.00");
    private static final Set<String> BLOCKED_PROJECT_STATUSES = Set.of("INACTIVE", "LOCKED", "CLOSED");

    private final QuotationRepository quotationRepository;
    private final ProjectRepository projectRepository;
    private final ProductRepository productRepository;
    private final PriceListRepository priceListRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogRepository auditLogRepository;
    private final QuotationMapper quotationMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public QuotationPreviewResponseData previewQuotation(QuotationSubmitRequest request) {
        CustomerProfileEntity customer = loadCurrentCustomer();
        PreparedQuotation preparedQuotation = prepareQuotation(customer, request, true, true);
        return quotationMapper.toPreviewResponse(
                customer.getId(),
                preparedQuotation.project() == null ? null : preparedQuotation.project().getId(),
                QuotationStatus.PENDING.name(),
                preparedQuotation.validUntil(),
                preparedQuotation.totalAmount(),
                normalizeNullable(request.getNote()),
                normalizeNullable(request.getDeliveryRequirement()),
                preparedQuotation.itemResponses()
        );
    }

    @Override
    @Transactional
    public QuotationResponseData createQuotation(QuotationSubmitRequest request) {
        CustomerProfileEntity customer = loadCurrentCustomer();
        PreparedQuotation preparedQuotation = prepareQuotation(customer, request, true, true);

        QuotationEntity quotation = buildQuotationEntity(
                customer,
                preparedQuotation.project(),
                preparedQuotation,
                QuotationStatus.PENDING,
                generateQuotationNumber()
        );

        QuotationEntity savedQuotation = quotationRepository.save(quotation);
        QuotationResponseData response = quotationMapper.toResponse(savedQuotation);
        logAudit("CREATE_QUOTATION", savedQuotation.getId(), response, customer.getUser().getId());
        return response;
    }

    @Override
    @Transactional
    public QuotationResponseData saveDraftQuotation(QuotationSubmitRequest request) {
        CustomerProfileEntity customer = loadCurrentCustomer();
        PreparedQuotation preparedQuotation = prepareQuotation(customer, request, false, false);

        QuotationEntity quotation = buildQuotationEntity(
                customer,
                preparedQuotation.project(),
                preparedQuotation,
                QuotationStatus.DRAFT,
                null
        );

        QuotationEntity savedQuotation = quotationRepository.save(quotation);
        QuotationResponseData response = quotationMapper.toResponse(savedQuotation);
        logAudit("SAVE_QUOTATION_DRAFT", savedQuotation.getId(), response, customer.getUser().getId());
        return response;
    }

    private CustomerProfileEntity loadCurrentCustomer() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (!RoleName.CUSTOMER.name().equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenOperationException("Only customer can create quotations");
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

    private PreparedQuotation prepareQuotation(
            CustomerProfileEntity customer,
            QuotationSubmitRequest request,
            boolean pricingRequired,
            boolean enforceMinimumAmount
    ) {
        validateDuplicateProducts(request);

        ProjectEntity project = resolveProject(customer.getId(), normalizeNullable(request.getProjectId()));

        Map<String, ProductEntity> products = loadProducts(request);
        List<PriceListEntity> applicablePriceLists = resolveApplicablePriceLists(customer, pricingRequired, request);
        Map<String, BigDecimal> unitPriceByProductId = mapUnitPrices(applicablePriceLists);

        List<PreparedQuotationItem> preparedItems = new ArrayList<>();
        List<QuotationItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (var itemRequest : request.getItems()) {
            ProductEntity product = products.get(itemRequest.getProductId().trim());
            validateProduct(product);

            BigDecimal unitPrice = unitPriceByProductId.get(product.getId());
            BigDecimal quantity = normalizeMoney(itemRequest.getQuantity());
            BigDecimal totalPrice = null;

            if (unitPrice == null) {
                if (pricingRequired) {
                    throw new QuotationPricingNotFoundException(product.getId());
                }
            } else {
                totalPrice = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                totalAmount = totalAmount.add(totalPrice).setScale(2, RoundingMode.HALF_UP);
            }

            preparedItems.add(new PreparedQuotationItem(product, quantity, unitPrice, totalPrice));
            itemResponses.add(quotationMapper.toItemResponse(null, product, quantity, unitPrice, totalPrice));
        }

        if (enforceMinimumAmount && totalAmount.compareTo(MIN_TOTAL_AMOUNT) < 0) {
            throw new QuotationAmountTooLowException();
        }

        return new PreparedQuotation(
                project,
                LocalDate.now(APP_ZONE).plusDays(15),
                totalAmount,
                preparedItems,
                itemResponses,
                normalizeNullable(request.getNote()),
                normalizeNullable(request.getDeliveryRequirement())
        );
    }

    private void validateDuplicateProducts(QuotationSubmitRequest request) {
        Set<String> uniqueProductIds = new java.util.HashSet<>();
        for (var item : request.getItems()) {
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

        if (StringUtils.hasText(project.getStatus())
                && BLOCKED_PROJECT_STATUSES.contains(project.getStatus().trim().toUpperCase())) {
            throw new QuotationProjectAccessException();
        }
        return project;
    }

    private Map<String, ProductEntity> loadProducts(QuotationSubmitRequest request) {
        List<String> productIds = request.getItems().stream()
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

    private List<PriceListEntity> resolveApplicablePriceLists(
            CustomerProfileEntity customer,
            boolean pricingRequired,
            QuotationSubmitRequest request
    ) {
        String customerGroup = normalizeNullable(customer.getCustomerType());
        if (!StringUtils.hasText(customerGroup)) {
            if (pricingRequired) {
                throw new QuotationPricingNotFoundException(request.getItems().get(0).getProductId().trim());
            }
            return List.of();
        }

        List<PriceListEntity> priceLists = priceListRepository.findApplicablePriceLists(
                customerGroup,
                LocalDate.now(APP_ZONE)
        );
        if (priceLists.isEmpty() && pricingRequired) {
            throw new QuotationPricingNotFoundException(request.getItems().get(0).getProductId().trim());
        }
        return priceLists;
    }

    private Map<String, BigDecimal> mapUnitPrices(List<PriceListEntity> applicablePriceLists) {
        Map<String, BigDecimal> unitPriceByProductId = new LinkedHashMap<>();
        if (applicablePriceLists.isEmpty()) {
            return unitPriceByProductId;
        }

        PriceListEntity selectedPriceList = applicablePriceLists.get(0);
        for (PriceListItemEntity item : selectedPriceList.getItems()) {
            if (item.getProduct() != null && item.getUnitPrice() != null) {
                unitPriceByProductId.putIfAbsent(item.getProduct().getId(), normalizeMoney(item.getUnitPrice()));
            }
        }
        return unitPriceByProductId;
    }

    private void validateProduct(ProductEntity product) {
        if (!ProductStatus.ACTIVE.name().equalsIgnoreCase(product.getStatus())) {
            throw RequestValidationException.singleError("productId", "Product must be ACTIVE: " + product.getId());
        }
    }

    private QuotationEntity buildQuotationEntity(
            CustomerProfileEntity customer,
            ProjectEntity project,
            PreparedQuotation preparedQuotation,
            QuotationStatus status,
            String quotationNumber
    ) {
        QuotationEntity quotation = new QuotationEntity();
        quotation.setCustomer(customer);
        quotation.setProject(project);
        quotation.setQuotationNumber(quotationNumber);
        quotation.setStatus(status.name());
        quotation.setValidUntil(preparedQuotation.validUntil());
        quotation.setTotalAmount(preparedQuotation.totalAmount());
        quotation.setNote(preparedQuotation.note());
        quotation.setDeliveryRequirement(preparedQuotation.deliveryRequirement());

        List<QuotationItemEntity> itemEntities = new ArrayList<>();
        for (PreparedQuotationItem preparedItem : preparedQuotation.items()) {
            QuotationItemEntity itemEntity = new QuotationItemEntity();
            itemEntity.setQuotation(quotation);
            itemEntity.setProduct(preparedItem.product());
            itemEntity.setQuantity(preparedItem.quantity());
            itemEntity.setUnitPrice(preparedItem.unitPrice());
            itemEntity.setTotalPrice(preparedItem.totalPrice());
            itemEntities.add(itemEntity);
        }
        quotation.setItems(itemEntities);
        return quotation;
    }

    private String generateQuotationNumber() {
        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        long sequence = quotationRepository.countByCreatedAtBetween(startOfDay, endOfDay) + 1;
        return "QT-" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%04d", sequence);
    }

    private void logAudit(String action, String entityId, Object newValue, String userId) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType("QUOTATION");
        auditLog.setEntityId(entityId);
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
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record PreparedQuotation(
            ProjectEntity project,
            LocalDate validUntil,
            BigDecimal totalAmount,
            List<PreparedQuotationItem> items,
            List<QuotationItemResponse> itemResponses,
            String note,
            String deliveryRequirement
    ) {
    }

    private record PreparedQuotationItem(
            ProductEntity product,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {
    }
}
