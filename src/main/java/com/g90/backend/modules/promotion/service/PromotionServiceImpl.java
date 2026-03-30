package com.g90.backend.modules.promotion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.InvalidDateRangeException;
import com.g90.backend.exception.PromotionDeletionNotAllowedException;
import com.g90.backend.exception.PromotionNotFoundException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.customer.entity.CustomerPriceGroup;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.entity.ProductStatus;
import com.g90.backend.modules.product.repository.ProductRepository;
import com.g90.backend.modules.promotion.dto.PromotionCreateDataResponse;
import com.g90.backend.modules.promotion.dto.PromotionCreateRequest;
import com.g90.backend.modules.promotion.dto.PromotionDetailResponse;
import com.g90.backend.modules.promotion.dto.PromotionListQuery;
import com.g90.backend.modules.promotion.dto.PromotionListResponseData;
import com.g90.backend.modules.promotion.dto.PromotionUpdateRequest;
import com.g90.backend.modules.promotion.entity.PromotionCustomerGroupEntity;
import com.g90.backend.modules.promotion.entity.PromotionEntity;
import com.g90.backend.modules.promotion.entity.PromotionProductEntity;
import com.g90.backend.modules.promotion.entity.PromotionStatus;
import com.g90.backend.modules.promotion.entity.PromotionType;
import com.g90.backend.modules.promotion.integration.PromotionNotificationGateway;
import com.g90.backend.modules.promotion.mapper.PromotionMapper;
import com.g90.backend.modules.promotion.repository.PromotionRepository;
import com.g90.backend.modules.promotion.repository.PromotionSpecifications;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ENTITY_TYPE = "PROMOTION";
    private static final Set<String> ACTIVE_ORDER_STATUSES = Set.of(
            QuotationStatus.PENDING.name(),
            QuotationStatus.APPROVED.name(),
            QuotationStatus.CONVERTED.name()
    );
    private static final DateTimeFormatter CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;
    private final QuotationRepository quotationRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AuditLogRepository auditLogRepository;
    private final PromotionMapper promotionMapper;
    private final PromotionNotificationGateway promotionNotificationGateway;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PromotionCreateDataResponse createPromotion(PromotionCreateRequest request) {
        AuthenticatedUser currentUser = ensureOwner();
        PromotionType promotionType = validateWriteRequest(
                request.getPromotionType(),
                request.getDiscountValue(),
                request.getValidFrom(),
                request.getValidTo(),
                request.getProductIds(),
                request.getCustomerGroups()
        );

        PromotionEntity entity = new PromotionEntity();
        entity.setCode(generatePromotionCode());
        applyHeader(
                entity,
                request.getName(),
                promotionType,
                request.getDiscountValue(),
                request.getValidFrom(),
                request.getValidTo(),
                request.getStatus(),
                request.getPriority(),
                request.getDescription(),
                currentUser.userId(),
                true
        );
        syncProductScopes(entity, request.getProductIds());
        syncCustomerGroupScopes(entity, request.getCustomerGroups());

        PromotionEntity saved = promotionRepository.save(entity);
        PromotionCreateDataResponse response = promotionMapper.toCreateData(saved);
        promotionNotificationGateway.notifyPromotionCreated(saved);
        logAudit("CREATE_PROMOTION", saved.getId(), null, promotionMapper.toDetailResponse(saved), currentUser.userId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionListResponseData getPromotions(PromotionListQuery query) {
        ViewerContext viewer = ensureCanView();
        normalizeAndValidateQuery(query, viewer);

        Page<PromotionEntity> page = promotionRepository.findAll(
                PromotionSpecifications.withFilters(query, viewer.internalView(), viewer.customerGroups()),
                PageRequest.of(normalizePage(query.getPage()) - 1, normalizeSize(query.getPageSize()), defaultSort())
        );
        return promotionMapper.toListResponse(page, query);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionDetailResponse getPromotionById(String id) {
        ViewerContext viewer = ensureCanView();
        PromotionEntity entity = findPromotion(id);
        if (!viewer.internalView() && !isVisibleToCustomer(entity, viewer.customerGroups())) {
            throw new PromotionNotFoundException();
        }
        return promotionMapper.toDetailResponse(entity);
    }

    @Override
    @Transactional
    public PromotionDetailResponse updatePromotion(String id, PromotionUpdateRequest request) {
        AuthenticatedUser currentUser = ensureOwner();
        PromotionType promotionType = validateWriteRequest(
                request.getPromotionType(),
                request.getDiscountValue(),
                request.getValidFrom(),
                request.getValidTo(),
                request.getProductIds(),
                request.getCustomerGroups()
        );

        PromotionEntity entity = findPromotion(id);
        PromotionDetailResponse oldState = promotionMapper.toDetailResponse(entity);

        applyHeader(
                entity,
                request.getName(),
                promotionType,
                request.getDiscountValue(),
                request.getValidFrom(),
                request.getValidTo(),
                request.getStatus(),
                request.getPriority(),
                request.getDescription(),
                currentUser.userId(),
                false
        );
        syncProductScopes(entity, request.getProductIds());
        syncCustomerGroupScopes(entity, request.getCustomerGroups());

        PromotionEntity saved = promotionRepository.save(entity);
        PromotionDetailResponse response = promotionMapper.toDetailResponse(saved);
        logAudit("UPDATE_PROMOTION", saved.getId(), oldState, response, currentUser.userId());
        return response;
    }

    @Override
    @Transactional
    public void deletePromotion(String id) {
        AuthenticatedUser currentUser = ensureOwner();
        PromotionEntity entity = findPromotion(id);
        if (StringUtils.hasText(entity.getCode())
                && quotationRepository.existsByPromotionCodeIgnoreCaseAndStatusIn(entity.getCode(), ACTIVE_ORDER_STATUSES)) {
            throw new PromotionDeletionNotAllowedException();
        }

        PromotionDetailResponse oldState = promotionMapper.toDetailResponse(entity);
        entity.setStatus(PromotionStatus.INACTIVE.name());
        entity.setUpdatedBy(currentUser.userId());
        entity.setDeletedAt(LocalDateTime.now(APP_ZONE));

        PromotionEntity saved = promotionRepository.save(entity);
        logAudit("DELETE_PROMOTION", saved.getId(), oldState, promotionMapper.toDetailResponse(saved), currentUser.userId());
    }

    private void applyHeader(
            PromotionEntity entity,
            String name,
            PromotionType promotionType,
            BigDecimal discountValue,
            LocalDate validFrom,
            LocalDate validTo,
            String status,
            Integer priority,
            String description,
            String userId,
            boolean creating
    ) {
        entity.setName(normalize(name));
        entity.setPromotionType(promotionType.name());
        entity.setDiscountValue(normalizeMoney(discountValue));
        entity.setStartDate(validFrom);
        entity.setEndDate(validTo);
        entity.setStatus(resolveStatus(status, creating ? PromotionStatus.ACTIVE : PromotionStatus.from(entity.getStatus())).name());
        entity.setPriority(priority == null ? 0 : priority);
        entity.setDescription(normalizeNullable(description));
        entity.setUpdatedBy(userId);
        if (creating) {
            entity.setCreatedBy(userId);
        }
    }

    private PromotionType validateWriteRequest(
            String promotionType,
            BigDecimal discountValue,
            LocalDate validFrom,
            LocalDate validTo,
            List<String> productIds,
            List<String> customerGroups
    ) {
        validateDateRange(validFrom, validTo);
        PromotionType resolvedType = resolveType(promotionType);
        validateDiscountValue(resolvedType, discountValue);
        sanitizeProductIds(productIds);
        sanitizeCustomerGroups(customerGroups);
        return resolvedType;
    }

    private void validateDateRange(LocalDate validFrom, LocalDate validTo) {
        if (validFrom != null && validTo != null && validFrom.isAfter(validTo)) {
            throw new InvalidDateRangeException();
        }
    }

    private void validateDiscountValue(PromotionType promotionType, BigDecimal discountValue) {
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw RequestValidationException.singleError("discountValue", "Discount value must be greater than 0");
        }
        if (promotionType == PromotionType.PERCENT && discountValue.compareTo(new BigDecimal("100")) > 0) {
            throw RequestValidationException.singleError("discountValue", "Percent discount must not exceed 100");
        }
    }

    private void syncProductScopes(PromotionEntity promotion, List<String> requestedProductIds) {
        if (promotion.getProducts() == null) {
            promotion.setProducts(new java.util.ArrayList<>());
        }

        List<String> productIds = sanitizeProductIds(requestedProductIds);
        Map<String, ProductEntity> productsById = loadProducts(productIds);
        Map<String, PromotionProductEntity> existingByProductId = activeProductScopesByProductId(promotion);
        Set<String> retainedProductIds = new LinkedHashSet<>();

        for (String productId : productIds) {
            PromotionProductEntity scope = existingByProductId.get(productId);
            if (scope == null) {
                scope = new PromotionProductEntity();
                promotion.getProducts().add(scope);
            }

            scope.setPromotion(promotion);
            scope.setProduct(productsById.get(productId));
            scope.setDeletedAt(null);
            retainedProductIds.add(productId);
        }

        for (PromotionProductEntity existing : activeProductScopes(promotion)) {
            String productId = existing.getProduct() == null ? null : existing.getProduct().getId();
            if (productId != null && !retainedProductIds.contains(productId)) {
                existing.setDeletedAt(LocalDateTime.now(APP_ZONE));
            }
        }
    }

    private void syncCustomerGroupScopes(PromotionEntity promotion, List<String> requestedCustomerGroups) {
        if (promotion.getCustomerGroups() == null) {
            promotion.setCustomerGroups(new java.util.ArrayList<>());
        }

        List<String> groups = sanitizeCustomerGroups(requestedCustomerGroups);
        Map<String, PromotionCustomerGroupEntity> existingByGroup = activeCustomerScopesByGroup(promotion);
        Set<String> retainedGroups = new LinkedHashSet<>();

        for (String group : groups) {
            PromotionCustomerGroupEntity scope = existingByGroup.get(group);
            if (scope == null) {
                scope = new PromotionCustomerGroupEntity();
                promotion.getCustomerGroups().add(scope);
            }

            scope.setPromotion(promotion);
            scope.setCustomerGroup(group);
            scope.setDeletedAt(null);
            retainedGroups.add(group);
        }

        for (PromotionCustomerGroupEntity existing : activeCustomerScopes(promotion)) {
            if (!retainedGroups.contains(existing.getCustomerGroup())) {
                existing.setDeletedAt(LocalDateTime.now(APP_ZONE));
            }
        }
    }

    private Map<String, ProductEntity> loadProducts(List<String> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        List<ProductEntity> products = productRepository.findAllById(productIds);
        Map<String, ProductEntity> productsById = new LinkedHashMap<>();
        products.forEach(product -> productsById.put(product.getId(), product));

        for (String productId : productIds) {
            ProductEntity product = productsById.get(productId);
            if (product == null || product.getDeletedAt() != null) {
                throw RequestValidationException.singleError("productIds", "Product not found: " + productId);
            }
            if (!ProductStatus.ACTIVE.name().equalsIgnoreCase(product.getStatus())) {
                throw RequestValidationException.singleError("productIds", "Product must be ACTIVE: " + productId);
            }
        }
        return productsById;
    }

    private Map<String, PromotionProductEntity> activeProductScopesByProductId(PromotionEntity promotion) {
        Map<String, PromotionProductEntity> result = new LinkedHashMap<>();
        for (PromotionProductEntity scope : activeProductScopes(promotion)) {
            if (scope.getProduct() != null && StringUtils.hasText(scope.getProduct().getId())) {
                result.put(scope.getProduct().getId(), scope);
            }
        }
        return result;
    }

    private Map<String, PromotionCustomerGroupEntity> activeCustomerScopesByGroup(PromotionEntity promotion) {
        Map<String, PromotionCustomerGroupEntity> result = new LinkedHashMap<>();
        for (PromotionCustomerGroupEntity scope : activeCustomerScopes(promotion)) {
            result.put(scope.getCustomerGroup(), scope);
        }
        return result;
    }

    private List<PromotionProductEntity> activeProductScopes(PromotionEntity promotion) {
        return promotion.getProducts().stream()
                .filter(scope -> scope.getDeletedAt() == null)
                .toList();
    }

    private List<PromotionCustomerGroupEntity> activeCustomerScopes(PromotionEntity promotion) {
        return promotion.getCustomerGroups().stream()
                .filter(scope -> scope.getDeletedAt() == null)
                .toList();
    }

    private void normalizeAndValidateQuery(PromotionListQuery query, ViewerContext viewer) {
        query.setSearch(normalizeNullable(query.getSearch()));
        query.setStatus(normalizeNullable(query.getStatus()));
        query.setPromotionType(normalizeNullable(query.getPromotionType()));
        query.setCustomerGroup(normalizeCustomerGroupNullable(query.getCustomerGroup()));
        query.setProductId(normalizeNullable(query.getProductId()));

        if (!viewer.internalView()) {
            query.setStatus(PromotionStatus.ACTIVE.name());
            query.setCustomerGroup(null);
        }

        if (StringUtils.hasText(query.getStatus())) {
            query.setStatus(resolveStatus(query.getStatus(), null).name());
        }
        if (StringUtils.hasText(query.getPromotionType())) {
            query.setPromotionType(resolveType(query.getPromotionType()).name());
        }
        if (query.getValidFrom() != null && query.getValidTo() != null && query.getValidFrom().isAfter(query.getValidTo())) {
            throw new InvalidDateRangeException();
        }
    }

    private boolean isVisibleToCustomer(PromotionEntity promotion, Set<String> customerGroups) {
        if (promotion.getDeletedAt() != null) {
            return false;
        }
        if (!PromotionStatus.ACTIVE.name().equalsIgnoreCase(promotion.getStatus())) {
            return false;
        }

        LocalDate today = LocalDate.now(APP_ZONE);
        if (promotion.getStartDate() != null && promotion.getStartDate().isAfter(today)) {
            return false;
        }
        if (promotion.getEndDate() != null && promotion.getEndDate().isBefore(today)) {
            return false;
        }
        return isApplicableToCustomerGroups(promotion, customerGroups);
    }

    private boolean isApplicableToCustomerGroups(PromotionEntity promotion, Set<String> customerGroups) {
        List<String> scopedGroups = activeCustomerScopes(promotion).stream()
                .map(PromotionCustomerGroupEntity::getCustomerGroup)
                .toList();
        if (scopedGroups.isEmpty()) {
            return true;
        }
        return scopedGroups.stream().anyMatch(customerGroups::contains);
    }

    private PromotionEntity findPromotion(String id) {
        return promotionRepository.findDetailedById(id).orElseThrow(PromotionNotFoundException::new);
    }

    private ViewerContext ensureCanView() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (RoleName.OWNER.name().equalsIgnoreCase(currentUser.role())
                || RoleName.ACCOUNTANT.name().equalsIgnoreCase(currentUser.role())) {
            return new ViewerContext(true, Set.of());
        }
        if (RoleName.CUSTOMER.name().equalsIgnoreCase(currentUser.role())) {
            return new ViewerContext(false, resolveCustomerGroups(currentUser.userId()));
        }
        throw new ForbiddenOperationException("You do not have permission to view promotions");
    }

    private AuthenticatedUser ensureOwner() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (!RoleName.OWNER.name().equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenOperationException("Only owner can manage promotions");
        }
        return currentUser;
    }

    private Set<String> resolveCustomerGroups(String userId) {
        Set<String> groups = new LinkedHashSet<>();
        groups.add(CustomerPriceGroup.DEFAULT);

        CustomerProfileEntity customer = customerProfileRepository.findByUser_Id(userId).orElse(null);
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

    private Sort defaultSort() {
        return Sort.by(
                Sort.Order.desc("priority"),
                Sort.Order.desc("updatedAt"),
                Sort.Order.desc("createdAt")
        );
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size < 1 ? 20 : size;
    }

    private PromotionStatus resolveStatus(String value, PromotionStatus defaultStatus) {
        if (!StringUtils.hasText(value)) {
            return defaultStatus == null ? PromotionStatus.ACTIVE : defaultStatus;
        }
        try {
            return PromotionStatus.from(value);
        } catch (IllegalArgumentException exception) {
            throw RequestValidationException.singleError("status", "Status must be DRAFT, ACTIVE, or INACTIVE");
        }
    }

    private PromotionType resolveType(String value) {
        try {
            return PromotionType.from(value);
        } catch (IllegalArgumentException exception) {
            throw RequestValidationException.singleError("promotionType", "Promotion type must be PERCENT or FIXED_AMOUNT");
        }
    }

    private List<String> sanitizeProductIds(Collection<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String productId : productIds) {
            String value = normalizeNullable(productId);
            if (value == null) {
                continue;
            }
            if (!normalized.add(value)) {
                throw RequestValidationException.singleError("productIds", "Duplicate product is not allowed");
            }
        }
        return List.copyOf(normalized);
    }

    private List<String> sanitizeCustomerGroups(Collection<String> customerGroups) {
        if (customerGroups == null || customerGroups.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String customerGroup : customerGroups) {
            String value = normalizeCustomerGroupNullable(customerGroup);
            if (value == null) {
                continue;
            }
            if (!normalized.add(value)) {
                throw RequestValidationException.singleError("customerGroups", "Duplicate customer group is not allowed");
            }
        }
        return List.copyOf(normalized);
    }

    private String generatePromotionCode() {
        for (int attempt = 0; attempt < 25; attempt++) {
            String candidate = "PROMO-" + LocalDateTime.now(APP_ZONE).format(CODE_FORMATTER) + "-" + randomSuffix();
            if (!promotionRepository.existsByCodeIgnoreCase(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate unique promotion code");
    }

    private String randomSuffix() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder builder = new StringBuilder(4);
        for (int index = 0; index < 4; index++) {
            builder.append(alphabet.charAt(ThreadLocalRandom.current().nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private void logAudit(String action, String entityId, Object oldValue, Object newValue, String userId) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType(ENTITY_TYPE);
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

    private String normalize(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeCustomerGroupNullable(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private record ViewerContext(boolean internalView, Set<String> customerGroups) {
    }
}
