package com.g90.backend.modules.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.DuplicateProductCodeException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.ProductNotFoundException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.product.dto.ProductCreateRequest;
import com.g90.backend.modules.product.dto.ProductListQuery;
import com.g90.backend.modules.product.dto.ProductListResponseData;
import com.g90.backend.modules.product.dto.ProductResponse;
import com.g90.backend.modules.product.dto.ProductStatusResponse;
import com.g90.backend.modules.product.dto.ProductStatusUpdateRequest;
import com.g90.backend.modules.product.dto.ProductUpdateRequest;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.entity.ProductImageEntity;
import com.g90.backend.modules.product.entity.ProductStatus;
import com.g90.backend.modules.product.mapper.ProductMapper;
import com.g90.backend.modules.product.repository.ProductRepository;
import com.g90.backend.modules.product.repository.ProductSpecifications;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductServiceImpl implements ProductService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ENTITY_TYPE = "PRODUCT";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "productCode", "productName");
    private static final Set<String> ALLOWED_SORT_DIRECTIONS = Set.of("asc", "desc");

    private final ProductRepository productRepository;
    private final AuditLogRepository auditLogRepository;
    private final ProductMapper productMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;
    private final ProductDeletionMode deletionMode;

    public ProductServiceImpl(
            ProductRepository productRepository,
            AuditLogRepository auditLogRepository,
            ProductMapper productMapper,
            CurrentUserProvider currentUserProvider,
            ObjectMapper objectMapper,
            @Value("${app.product.deletion.mode:WAREHOUSE_SOFT_DEACTIVATE}") String deletionMode
    ) {
        this.productRepository = productRepository;
        this.auditLogRepository = auditLogRepository;
        this.productMapper = productMapper;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
        this.deletionMode = ProductDeletionMode.fromProperty(deletionMode);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductListResponseData getProducts(ProductListQuery query) {
        normalizeAndValidateQuery(query);
        boolean warehouseView = isWarehouse(currentUser());

        if (!warehouseView) {
            query.setStatus(ProductStatus.ACTIVE.name());
        }

        Page<ProductEntity> page = productRepository.findAll(
                ProductSpecifications.withFilters(query, warehouseView),
                PageRequest.of(query.getPage() - 1, query.getPageSize(), buildSort(query))
        );
        return productMapper.toListResponse(page, query);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        AuthenticatedUser currentUser = ensureWarehouse();
        String normalizedProductCode = normalize(request.getProductCode());
        assertUniqueProductCode(normalizedProductCode, null);

        ProductEntity product = productMapper.toEntity(request);
        applyEditableFields(
                product,
                normalizedProductCode,
                request.getProductName(),
                request.getType(),
                request.getSize(),
                request.getThickness(),
                request.getUnit(),
                request.getWeightConversion(),
                request.getReferenceWeight(),
                request.getDescription(),
                request.getStatus(),
                false
        );
        product.setCreatedBy(currentUser.userId());
        product.setUpdatedBy(currentUser.userId());
        syncImages(product, request.getImageUrls());

        ProductEntity saved = productRepository.save(product);
        ProductResponse response = productMapper.toResponse(saved);
        logAudit("CREATE_PRODUCT", saved.getId(), null, response, currentUser.userId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(String id) {
        ProductEntity product = findProduct(id);
        if (!canViewInternalProduct(product)) {
            throw new ProductNotFoundException();
        }
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(String id, ProductUpdateRequest request) {
        AuthenticatedUser currentUser = ensureWarehouse();
        ProductEntity product = findProduct(id);
        ensureMutable(product);

        String normalizedProductCode = normalize(request.getProductCode());
        assertUniqueProductCode(normalizedProductCode, product.getId());

        ProductResponse oldState = productMapper.toResponse(product);
        productMapper.updateEntity(product, request);
        applyEditableFields(
                product,
                normalizedProductCode,
                request.getProductName(),
                request.getType(),
                request.getSize(),
                request.getThickness(),
                request.getUnit(),
                request.getWeightConversion(),
                request.getReferenceWeight(),
                request.getDescription(),
                request.getStatus(),
                true
        );
        product.setUpdatedBy(currentUser.userId());
        syncImages(product, request.getImageUrls());

        ProductEntity saved = productRepository.save(product);
        ProductResponse response = productMapper.toResponse(saved);
        logAudit("UPDATE_PRODUCT", saved.getId(), oldState, response, currentUser.userId());
        return response;
    }

    @Override
    @Transactional
    public ProductStatusResponse updateProductStatus(String id, ProductStatusUpdateRequest request) {
        AuthenticatedUser currentUser = ensureWarehouse();
        ProductEntity product = findProduct(id);
        ensureMutable(product);

        ProductStatusResponse oldState = productMapper.toStatusResponse(product);
        product.setStatus(resolveStatus(request.getStatus(), true).name());
        product.setUpdatedBy(currentUser.userId());

        ProductEntity saved = productRepository.save(product);
        ProductStatusResponse response = productMapper.toStatusResponse(saved);
        logAudit("UPDATE_PRODUCT_STATUS", saved.getId(), oldState, response, currentUser.userId());
        return response;
    }

    @Override
    @Transactional
    public ProductStatusResponse deleteProduct(String id) {
        AuthenticatedUser currentUser = ensureDeletionActor();
        ProductEntity product = findProduct(id);

        if (product.getDeletedAt() != null) {
            return productMapper.toStatusResponse(product);
        }

        ProductResponse oldState = productMapper.toResponse(product);
        product.setStatus(ProductStatus.INACTIVE.name());
        product.setDeletedAt(LocalDateTime.now(APP_ZONE));
        product.setUpdatedBy(currentUser.userId());

        ProductEntity saved = productRepository.save(product);
        ProductStatusResponse response = productMapper.toStatusResponse(saved);
        logAudit("DELETE_PRODUCT", saved.getId(), oldState, productMapper.toResponse(saved), currentUser.userId());
        return response;
    }

    private ProductEntity findProduct(String id) {
        return productRepository.findDetailedById(id).orElseThrow(ProductNotFoundException::new);
    }

    private void applyEditableFields(
            ProductEntity product,
            String productCode,
            String productName,
            String type,
            String size,
            String thickness,
            String unit,
            BigDecimal weightConversion,
            BigDecimal referenceWeight,
            String description,
            String status,
            boolean statusRequired
    ) {
        product.setProductCode(productCode);
        product.setProductName(normalize(productName));
        product.setType(normalize(type));
        product.setSize(normalize(size));
        product.setThickness(normalize(thickness));
        product.setUnit(normalize(unit));
        product.setWeightConversion(normalizeDecimal(weightConversion));
        product.setReferenceWeight(normalizeDecimal(referenceWeight));
        product.setDescription(normalizeNullable(description));
        product.setStatus(resolveStatus(status, statusRequired).name());
    }

    private void syncImages(ProductEntity product, List<String> requestedImageUrls) {
        if (product.getImages() == null) {
            product.setImages(new ArrayList<>());
        } else {
            product.getImages().clear();
        }

        List<String> imageUrls = sanitizeImageUrls(requestedImageUrls);
        for (int index = 0; index < imageUrls.size(); index++) {
            ProductImageEntity image = new ProductImageEntity();
            image.setProduct(product);
            image.setImageUrl(imageUrls.get(index));
            image.setDisplayOrder(index + 1);
            product.getImages().add(image);
        }
    }

    private void assertUniqueProductCode(String productCode, String currentProductId) {
        Optional<ProductEntity> existingProduct = productRepository.findByProductCodeIgnoreCase(productCode);
        if (existingProduct.isPresent() && !existingProduct.get().getId().equals(currentProductId)) {
            throw new DuplicateProductCodeException();
        }
    }

    private void normalizeAndValidateQuery(ProductListQuery query) {
        String keyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword() : query.getSearch();
        String size = StringUtils.hasText(query.getSize()) ? query.getSize() : query.getSizeValue();

        query.setKeyword(normalizeNullable(keyword));
        query.setSearch(query.getKeyword());
        query.setType(normalizeNullable(query.getType()));
        query.setSize(normalizeNullable(size));
        query.setSizeValue(query.getSize());
        query.setThickness(normalizeNullable(query.getThickness()));
        query.setUnit(normalizeNullable(query.getUnit()));
        query.setStatus(normalizeNullable(query.getStatus()));
        query.setSortBy(StringUtils.hasText(query.getSortBy()) ? query.getSortBy().trim() : "createdAt");
        query.setSortDir(StringUtils.hasText(query.getSortDir()) ? query.getSortDir().trim().toLowerCase(Locale.ROOT) : "desc");

        if (StringUtils.hasText(query.getStatus())) {
            query.setStatus(resolveStatus(query.getStatus(), true).name());
        }
        if (!ALLOWED_SORT_FIELDS.contains(query.getSortBy())) {
            throw RequestValidationException.singleError("sortBy", "sortBy must be one of createdAt, updatedAt, productCode, productName");
        }
        if (!ALLOWED_SORT_DIRECTIONS.contains(query.getSortDir())) {
            throw RequestValidationException.singleError("sortDir", "sortDir must be asc or desc");
        }
    }

    private Sort buildSort(ProductListQuery query) {
        Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, query.getSortBy());
    }

    private ProductStatus resolveStatus(String status, boolean required) {
        if (!StringUtils.hasText(status)) {
            if (required) {
                throw RequestValidationException.singleError("status", "status must be ACTIVE or INACTIVE");
            }
            return ProductStatus.ACTIVE;
        }

        try {
            return ProductStatus.from(status);
        } catch (IllegalArgumentException exception) {
            throw RequestValidationException.singleError("status", "status must be ACTIVE or INACTIVE");
        }
    }

    private void ensureMutable(ProductEntity product) {
        if (product.getDeletedAt() != null) {
            throw RequestValidationException.singleError("id", "Archived products cannot be modified");
        }
    }

    private boolean canViewInternalProduct(ProductEntity product) {
        if (isWarehouse(currentUser())) {
            return true;
        }
        return product.getDeletedAt() == null && ProductStatus.ACTIVE.name().equalsIgnoreCase(product.getStatus());
    }

    private AuthenticatedUser ensureWarehouse() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (!isWarehouse(currentUser)) {
            throw new ForbiddenOperationException("Only warehouse users can manage products");
        }
        return currentUser;
    }

    private AuthenticatedUser ensureDeletionActor() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        boolean warehouse = isWarehouse(currentUser);
        boolean owner = currentUser != null && RoleName.OWNER.name().equalsIgnoreCase(currentUser.role());

        return switch (deletionMode) {
            case WAREHOUSE_SOFT_DEACTIVATE -> {
                if (!warehouse && !owner) {
                    throw new ForbiddenOperationException("You do not have permission to archive products");
                }
                yield currentUser;
            }
            case OWNER_APPROVAL_SOFT_DEACTIVATE -> {
                if (owner) {
                    yield currentUser;
                }
                if (warehouse) {
                    throw new ForbiddenOperationException("Product deletion requires owner approval by policy");
                }
                throw new ForbiddenOperationException("You do not have permission to archive products");
            }
        };
    }

    private boolean isWarehouse(AuthenticatedUser currentUser) {
        return currentUser != null && RoleName.WAREHOUSE.name().equalsIgnoreCase(currentUser.role());
    }

    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user;
    }

    private BigDecimal normalizeDecimal(BigDecimal value) {
        return value == null ? null : value.setScale(4, RoundingMode.HALF_UP);
    }

    private List<String> sanitizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String imageUrl : imageUrls) {
            String value = normalizeNullable(imageUrl);
            if (value != null) {
                normalized.add(value);
            }
        }
        return new ArrayList<>(normalized);
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
