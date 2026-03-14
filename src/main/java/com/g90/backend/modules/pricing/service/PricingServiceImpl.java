package com.g90.backend.modules.pricing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.InvalidDateRangeException;
import com.g90.backend.exception.PriceListItemNotFoundException;
import com.g90.backend.exception.PriceListNotFoundException;
import com.g90.backend.exception.PricingProductNotFoundException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.pricing.dto.PriceListCreateDataResponse;
import com.g90.backend.modules.pricing.dto.PriceListCreateRequest;
import com.g90.backend.modules.pricing.dto.PriceListDetailResponse;
import com.g90.backend.modules.pricing.dto.PriceListItemCreateDataResponse;
import com.g90.backend.modules.pricing.dto.PriceListItemCreateRequest;
import com.g90.backend.modules.pricing.dto.PriceListItemResponse;
import com.g90.backend.modules.pricing.dto.PriceListItemUpdateRequest;
import com.g90.backend.modules.pricing.dto.PriceListListQuery;
import com.g90.backend.modules.pricing.dto.PriceListListResponseData;
import com.g90.backend.modules.pricing.dto.PriceListUpdateRequest;
import com.g90.backend.modules.pricing.entity.PriceListEntity;
import com.g90.backend.modules.pricing.entity.PriceListItemEntity;
import com.g90.backend.modules.pricing.entity.PriceListStatus;
import com.g90.backend.modules.pricing.mapper.PricingMapper;
import com.g90.backend.modules.pricing.repository.PriceListItemRepository;
import com.g90.backend.modules.pricing.repository.PriceListRepository;
import com.g90.backend.modules.pricing.repository.PricingSpecifications;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

    private final PriceListRepository priceListRepository;
    private final PriceListItemRepository priceListItemRepository;
    private final ProductRepository productRepository;
    private final AuditLogRepository auditLogRepository;
    private final PricingMapper pricingMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PriceListCreateDataResponse createPriceList(PriceListCreateRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());

        PriceListEntity entity = pricingMapper.toEntity(request);
        entity.setName(normalize(request.getName()));
        entity.setCustomerGroup(normalizeNullable(request.getCustomerGroup()));
        entity.setStatus(resolveStatus(request.getStatus(), false).name());

        PriceListEntity saved = priceListRepository.save(entity);
        logAudit("CREATE_PRICE_LIST", "PRICE_LIST", saved.getId(), null, pricingMapper.toDetailResponse(saved));
        return pricingMapper.toCreateData(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceListListResponseData getPriceLists(PriceListListQuery query) {
        normalizeAndValidateQuery(query);
        Page<PriceListEntity> page = priceListRepository.findAll(
                PricingSpecifications.withFilters(query),
                PageRequest.of(query.getPage(), query.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return pricingMapper.toListResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceListDetailResponse getPriceListById(String id) {
        return pricingMapper.toDetailResponse(findPriceListWithItems(id));
    }

    @Override
    @Transactional
    public void updatePriceList(String id, PriceListUpdateRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());

        PriceListEntity entity = findPriceListWithItems(id);
        PriceListDetailResponse oldState = pricingMapper.toDetailResponse(entity);

        pricingMapper.updateEntity(entity, request);
        entity.setName(normalize(request.getName()));
        entity.setCustomerGroup(normalizeNullable(request.getCustomerGroup()));
        entity.setStatus(resolveStatus(request.getStatus(), true).name());

        PriceListEntity saved = priceListRepository.save(entity);
        logAudit("UPDATE_PRICE_LIST", "PRICE_LIST", saved.getId(), oldState, pricingMapper.toDetailResponse(saved));
    }

    @Override
    @Transactional
    public void deletePriceList(String id) {
        PriceListEntity entity = findPriceListWithItems(id);
        PriceListDetailResponse oldState = pricingMapper.toDetailResponse(entity);
        priceListRepository.delete(entity);
        logAudit("DELETE_PRICE_LIST", "PRICE_LIST", id, oldState, null);
    }

    @Override
    @Transactional
    public PriceListItemCreateDataResponse addPriceListItem(String priceListId, PriceListItemCreateRequest request) {
        PriceListEntity priceList = findPriceList(priceListId);
        ProductEntity product = findProduct(request.getProductId());

        PriceListItemEntity entity = new PriceListItemEntity();
        entity.setPriceList(priceList);
        entity.setProduct(product);
        entity.setUnitPrice(normalizeMoney(request.getUnitPrice()));

        PriceListItemEntity saved = priceListItemRepository.save(entity);
        logAudit("CREATE_PRICE_LIST_ITEM", "PRICE_LIST_ITEM", saved.getId(), null, pricingMapper.toItemResponse(saved));
        return pricingMapper.toItemCreateData(saved);
    }

    @Override
    @Transactional
    public void updatePriceListItem(String itemId, PriceListItemUpdateRequest request) {
        PriceListItemEntity entity = findPriceListItem(itemId);
        PriceListItemResponse oldState = pricingMapper.toItemResponse(entity);

        entity.setUnitPrice(normalizeMoney(request.getUnitPrice()));
        PriceListItemEntity saved = priceListItemRepository.save(entity);
        logAudit("UPDATE_PRICE_LIST_ITEM", "PRICE_LIST_ITEM", saved.getId(), oldState, pricingMapper.toItemResponse(saved));
    }

    @Override
    @Transactional
    public void deletePriceListItem(String itemId) {
        PriceListItemEntity entity = findPriceListItem(itemId);
        PriceListItemResponse oldState = pricingMapper.toItemResponse(entity);
        priceListItemRepository.delete(entity);
        logAudit("DELETE_PRICE_LIST_ITEM", "PRICE_LIST_ITEM", itemId, oldState, null);
    }

    private PriceListEntity findPriceList(String id) {
        return priceListRepository.findById(id).orElseThrow(PriceListNotFoundException::new);
    }

    private PriceListEntity findPriceListWithItems(String id) {
        return priceListRepository.findWithItemsById(id).orElseThrow(PriceListNotFoundException::new);
    }

    private PriceListItemEntity findPriceListItem(String id) {
        return priceListItemRepository.findById(id).orElseThrow(PriceListItemNotFoundException::new);
    }

    private ProductEntity findProduct(String productId) {
        return productRepository.findById(productId).orElseThrow(PricingProductNotFoundException::new);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && !startDate.isBefore(endDate)) {
            throw new InvalidDateRangeException();
        }
    }

    private void normalizeAndValidateQuery(PriceListListQuery query) {
        query.setStatus(normalizeNullable(query.getStatus()));
        query.setCustomerGroup(normalizeNullable(query.getCustomerGroup()));

        if (StringUtils.hasText(query.getStatus())) {
            query.setStatus(resolveStatus(query.getStatus(), true).name());
        }
    }

    private PriceListStatus resolveStatus(String status, boolean required) {
        if (!StringUtils.hasText(status)) {
            if (required) {
                throw RequestValidationException.singleError("status", "status must be ACTIVE or INACTIVE");
            }
            return PriceListStatus.ACTIVE;
        }

        try {
            return PriceListStatus.from(status);
        } catch (IllegalArgumentException exception) {
            throw RequestValidationException.singleError("status", "status must be ACTIVE or INACTIVE");
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void logAudit(String action, String entityType, String entityId, Object oldValue, Object newValue) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(null);
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

    private String normalize(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
