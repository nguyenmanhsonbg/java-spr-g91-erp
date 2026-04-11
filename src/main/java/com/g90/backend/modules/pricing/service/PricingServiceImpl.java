package com.g90.backend.modules.pricing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.InvalidDateRangeException;
import com.g90.backend.exception.PriceListDeletionNotAllowedException;
import com.g90.backend.exception.PriceListNotFoundException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.contract.entity.ContractStatus;
import com.g90.backend.modules.contract.repository.ContractRepository;
import com.g90.backend.modules.pricing.dto.PriceListCreateDataResponse;
import com.g90.backend.modules.pricing.dto.PriceListCreateRequest;
import com.g90.backend.modules.pricing.dto.PriceListDetailResponse;
import com.g90.backend.modules.pricing.dto.PriceListItemWriteRequest;
import com.g90.backend.modules.pricing.dto.PriceListListQuery;
import com.g90.backend.modules.pricing.dto.PriceListListResponseData;
import com.g90.backend.modules.pricing.dto.PriceListUpdateRequest;
import com.g90.backend.modules.pricing.entity.PriceListEntity;
import com.g90.backend.modules.pricing.entity.PriceListItemEntity;
import com.g90.backend.modules.pricing.entity.PriceListStatus;
import com.g90.backend.modules.pricing.mapper.PricingMapper;
import com.g90.backend.modules.pricing.repository.PriceListRepository;
import com.g90.backend.modules.pricing.repository.PricingSpecifications;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.repository.ProductRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
public class PricingServiceImpl implements PricingService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ENTITY_TYPE = "PRICE_LIST";
    private static final Set<String> ACTIVE_ORDER_STATUSES = Set.of(
            ContractStatus.PENDING_CUSTOMER_APPROVAL.name(),
            ContractStatus.CUSTOMER_APPROVAL.name(),
            ContractStatus.PENDING_APPROVAL.name(),
            ContractStatus.APPROVED.name(),
            ContractStatus.SUBMITTED.name(),
            ContractStatus.PROCESSING.name(),
            ContractStatus.RESERVED.name(),
            ContractStatus.PICKED.name(),
            ContractStatus.IN_TRANSIT.name(),
            ContractStatus.DELIVERED.name()
    );

    private final PriceListRepository priceListRepository;
    private final ProductRepository productRepository;
    private final ContractRepository contractRepository;
    private final AuditLogRepository auditLogRepository;
    private final PricingMapper pricingMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PriceListCreateDataResponse createPriceList(PriceListCreateRequest request) {
        AuthenticatedUser currentUser = ensureOwner();
        validateRequest(request.getValidFrom(), request.getValidTo(), request.getItems());

        PriceListEntity entity = new PriceListEntity();
        applyHeader(
                entity,
                request.getName(),
                request.getCustomerGroup(),
                request.getValidFrom(),
                request.getValidTo(),
                request.getStatus(),
                currentUser.userId(),
                true
        );
        syncItems(entity, request.getItems());

        PriceListEntity saved = priceListRepository.save(entity);
        logAudit("CREATE_PRICE_LIST", saved.getId(), null, pricingMapper.toDetailResponse(saved), currentUser.userId());
        return pricingMapper.toCreateData(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceListListResponseData getPriceLists(PriceListListQuery query) {
        ensureCanView();
        normalizeAndValidateQuery(query);

        Page<PriceListEntity> page = priceListRepository.findAll(
                PricingSpecifications.withFilters(query),
                PageRequest.of(normalizePage(query.getPage()) - 1, normalizeSize(query.getPageSize()), Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return pricingMapper.toListResponse(page, query);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceListDetailResponse getPriceListById(String id) {
        ensureCanView();
        return pricingMapper.toDetailResponse(findPriceList(id));
    }

    @Override
    @Transactional
    public PriceListDetailResponse updatePriceList(String id, PriceListUpdateRequest request) {
        AuthenticatedUser currentUser = ensureOwner();
        validateRequest(request.getValidFrom(), request.getValidTo(), request.getItems());

        PriceListEntity entity = findPriceList(id);
        PriceListDetailResponse oldState = pricingMapper.toDetailResponse(entity);

        applyHeader(
                entity,
                request.getName(),
                request.getCustomerGroup(),
                request.getValidFrom(),
                request.getValidTo(),
                request.getStatus(),
                currentUser.userId(),
                false
        );
        syncItems(entity, request.getItems());

        PriceListEntity saved = priceListRepository.save(entity);
        PriceListDetailResponse newState = pricingMapper.toDetailResponse(saved);
        logAudit("UPDATE_PRICE_LIST", saved.getId(), oldState, newState, currentUser.userId());
        return newState;
    }

    @Override
    @Transactional
    public void deletePriceList(String id) {
        AuthenticatedUser currentUser = ensureOwner();
        PriceListEntity entity = findPriceList(id);
        if (contractRepository.existsByPriceListIdAndStatusIn(id, ACTIVE_ORDER_STATUSES)) {
            throw new PriceListDeletionNotAllowedException();
        }

        PriceListDetailResponse oldState = pricingMapper.toDetailResponse(entity);
        entity.setStatus(PriceListStatus.INACTIVE.name());
        entity.setUpdatedBy(currentUser.userId());
        entity.setDeletedAt(LocalDateTime.now(APP_ZONE));

        PriceListEntity saved = priceListRepository.save(entity);
        logAudit("DELETE_PRICE_LIST", saved.getId(), oldState, pricingMapper.toDetailResponse(saved), currentUser.userId());
    }

    private void applyHeader(
            PriceListEntity entity,
            String name,
            String customerGroup,
            LocalDate validFrom,
            LocalDate validTo,
            String status,
            String userId,
            boolean creating
    ) {
        entity.setName(normalize(name));
        entity.setCustomerGroup(normalize(customerGroup));
        entity.setValidFrom(validFrom);
        entity.setValidTo(validTo);
        entity.setStatus(resolveStatus(status, creating ? PriceListStatus.ACTIVE : PriceListStatus.from(entity.getStatus())).name());
        entity.setUpdatedBy(userId);
        if (creating) {
            entity.setCreatedBy(userId);
        }
    }

    private void syncItems(PriceListEntity priceList, List<PriceListItemWriteRequest> requestedItems) {
        validateDuplicateProducts(requestedItems);
        Map<String, ProductEntity> productsById = loadProducts(requestedItems);
        Map<String, PriceListItemEntity> existingActiveItemsById = activeItemsById(priceList);
        Set<String> retainedItemIds = new LinkedHashSet<>();

        for (int index = 0; index < requestedItems.size(); index++) {
            PriceListItemWriteRequest requestItem = requestedItems.get(index);
            PriceListItemEntity entity = resolveItemEntity(priceList, requestItem, existingActiveItemsById, index);
            ProductEntity product = productsById.get(requestItem.getProductId().trim());

            entity.setPriceList(priceList);
            entity.setProduct(product);
            entity.setUnitPrice(normalizeMoney(requestItem.getUnitPriceVnd()));
            entity.setPricingRuleType(normalizeNullable(requestItem.getPricingRuleType()));
            entity.setNote(normalizeNullable(requestItem.getNote()));
            entity.setDeletedAt(null);

            if (!priceList.getItems().contains(entity)) {
                priceList.getItems().add(entity);
            }
            if (StringUtils.hasText(entity.getId())) {
                retainedItemIds.add(entity.getId());
            }
        }

        for (PriceListItemEntity existingItem : activeItems(priceList)) {
            boolean keepExisting = StringUtils.hasText(existingItem.getId()) && retainedItemIds.contains(existingItem.getId());
            boolean matchedUnsavedItem = !StringUtils.hasText(existingItem.getId()) && containsUnsavedProduct(requestedItems, existingItem);
            if (!keepExisting && !matchedUnsavedItem) {
                existingItem.setDeletedAt(LocalDateTime.now(APP_ZONE));
            }
        }
    }

    private PriceListItemEntity resolveItemEntity(
            PriceListEntity priceList,
            PriceListItemWriteRequest requestItem,
            Map<String, PriceListItemEntity> existingActiveItemsById,
            int index
    ) {
        if (!StringUtils.hasText(requestItem.getId())) {
            return new PriceListItemEntity();
        }

        PriceListItemEntity entity = existingActiveItemsById.get(requestItem.getId().trim());
        if (entity == null) {
            throw RequestValidationException.singleError("items[" + index + "].id", "Price list item does not belong to this price list");
        }
        return entity;
    }

    private boolean containsUnsavedProduct(List<PriceListItemWriteRequest> requestedItems, PriceListItemEntity existingItem) {
        if (existingItem.getProduct() == null) {
            return false;
        }
        String productId = existingItem.getProduct().getId();
        return requestedItems.stream()
                .filter(item -> !StringUtils.hasText(item.getId()))
                .anyMatch(item -> productId.equals(item.getProductId().trim()));
    }

    private void validateRequest(LocalDate validFrom, LocalDate validTo, List<PriceListItemWriteRequest> items) {
        validateDateRange(validFrom, validTo);
        if (items == null || items.isEmpty()) {
            throw RequestValidationException.singleError("items", "At least one pricing item is required");
        }
    }

    private void validateDateRange(LocalDate validFrom, LocalDate validTo) {
        if (validFrom != null && validTo != null && validFrom.isAfter(validTo)) {
            throw new InvalidDateRangeException();
        }
    }

    private void validateDuplicateProducts(List<PriceListItemWriteRequest> items) {
        Set<String> uniqueProductIds = new LinkedHashSet<>();
        for (int index = 0; index < items.size(); index++) {
            PriceListItemWriteRequest item = items.get(index);
            String productId = item.getProductId().trim();
            if (!uniqueProductIds.add(productId)) {
                throw RequestValidationException.singleError("items[" + index + "].productId", "Duplicate product is not allowed");
            }
        }
    }

    private Map<String, ProductEntity> loadProducts(List<PriceListItemWriteRequest> requestedItems) {
        List<String> productIds = requestedItems.stream()
                .map(item -> item.getProductId().trim())
                .toList();

        List<ProductEntity> products = productRepository.findAllById(productIds);
        Map<String, ProductEntity> productsById = new LinkedHashMap<>();
        products.forEach(product -> productsById.put(product.getId(), product));

        for (int index = 0; index < requestedItems.size(); index++) {
            String productId = requestedItems.get(index).getProductId().trim();
            if (!productsById.containsKey(productId)) {
                throw RequestValidationException.singleError("items[" + index + "].productId", "Product not found");
            }
        }
        return productsById;
    }

    private PriceListEntity findPriceList(String id) {
        return priceListRepository.findDetailedById(id).orElseThrow(PriceListNotFoundException::new);
    }

    private Map<String, PriceListItemEntity> activeItemsById(PriceListEntity priceList) {
        Map<String, PriceListItemEntity> result = new LinkedHashMap<>();
        for (PriceListItemEntity item : activeItems(priceList)) {
            if (StringUtils.hasText(item.getId())) {
                result.put(item.getId(), item);
            }
        }
        return result;
    }

    private List<PriceListItemEntity> activeItems(PriceListEntity priceList) {
        return priceList.getItems().stream()
                .filter(item -> item.getDeletedAt() == null)
                .toList();
    }

    private void normalizeAndValidateQuery(PriceListListQuery query) {
        query.setSearch(normalizeNullable(query.getSearch()));
        query.setStatus(normalizeNullable(query.getStatus()));
        query.setCustomerGroup(normalizeNullable(query.getCustomerGroup()));

        if (StringUtils.hasText(query.getStatus())) {
            query.setStatus(resolveStatus(query.getStatus(), null).name());
        }
        if (query.getValidFrom() != null && query.getValidTo() != null && query.getValidFrom().isAfter(query.getValidTo())) {
            throw new InvalidDateRangeException();
        }
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size < 1 ? 20 : size;
    }

    private PriceListStatus resolveStatus(String status, PriceListStatus defaultStatus) {
        if (!StringUtils.hasText(status)) {
            return defaultStatus == null ? PriceListStatus.ACTIVE : defaultStatus;
        }

        try {
            return PriceListStatus.from(status);
        } catch (IllegalArgumentException exception) {
            throw RequestValidationException.singleError("status", "Status must be ACTIVE or INACTIVE");
        }
    }

    private AuthenticatedUser ensureOwner() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (!RoleName.OWNER.name().equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenOperationException("Only owner can manage price lists");
        }
        return currentUser;
    }

    private void ensureCanView() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (!RoleName.OWNER.name().equalsIgnoreCase(currentUser.role())
                && !RoleName.ACCOUNTANT.name().equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenOperationException("You do not have permission to view price lists");
        }
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
}
