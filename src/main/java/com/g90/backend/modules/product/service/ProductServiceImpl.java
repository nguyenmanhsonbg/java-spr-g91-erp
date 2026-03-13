package com.g90.backend.modules.product.service;

import com.g90.backend.exception.DuplicateProductCodeException;
import com.g90.backend.exception.ProductListLoadException;
import com.g90.backend.exception.ProductNotFoundException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.product.dto.ProductCreateRequest;
import com.g90.backend.modules.product.dto.ProductListQuery;
import com.g90.backend.modules.product.dto.ProductListResponseData;
import com.g90.backend.modules.product.dto.ProductResponse;
import com.g90.backend.modules.product.dto.ProductStatusResponse;
import com.g90.backend.modules.product.dto.ProductStatusUpdateRequest;
import com.g90.backend.modules.product.dto.ProductUpdateRequest;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.entity.ProductStatus;
import com.g90.backend.modules.product.mapper.ProductMapper;
import com.g90.backend.modules.product.repository.ProductRepository;
import com.g90.backend.modules.product.repository.ProductSpecifications;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
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
public class ProductServiceImpl implements ProductService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "productCode", "productName");
    private static final Set<String> ALLOWED_SORT_DIRECTIONS = Set.of("asc", "desc");

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public ProductListResponseData getProducts(ProductListQuery query) {
        try {
            normalizeAndValidateQuery(query);
            Page<ProductEntity> page = productRepository.findAll(
                    ProductSpecifications.withFilters(query),
                    PageRequest.of(query.getPage() - 1, query.getPageSize(), buildSort(query))
            );
            return productMapper.toListResponse(page, query);
        } catch (RequestValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ProductListLoadException();
        }
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        String normalizedProductCode = normalizeText(request.getProductCode());
        assertUniqueProductCode(normalizedProductCode, null);

        ProductEntity product = productMapper.toEntity(request);
        product.setProductCode(normalizedProductCode);
        product.setProductName(normalizeText(request.getProductName()));
        product.setType(normalizeText(request.getType()));
        product.setSize(normalizeText(request.getSize()));
        product.setThickness(normalizeText(request.getThickness()));
        product.setUnit(normalizeText(request.getUnit()));
        product.setWeightConversion(normalizeDecimal(request.getWeightConversion()));
        product.setReferenceWeight(normalizeDecimal(request.getReferenceWeight()));
        product.setStatus(resolveStatus(request.getStatus(), false));

        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(String id) {
        return productMapper.toResponse(findProduct(id));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(String id, ProductUpdateRequest request) {
        ProductEntity product = findProduct(id);
        String normalizedProductCode = normalizeText(request.getProductCode());
        assertUniqueProductCode(normalizedProductCode, product.getId());

        productMapper.updateEntity(product, request);
        product.setProductCode(normalizedProductCode);
        product.setProductName(normalizeText(request.getProductName()));
        product.setType(normalizeText(request.getType()));
        product.setSize(normalizeText(request.getSize()));
        product.setThickness(normalizeText(request.getThickness()));
        product.setUnit(normalizeText(request.getUnit()));
        product.setWeightConversion(normalizeDecimal(request.getWeightConversion()));
        product.setReferenceWeight(normalizeDecimal(request.getReferenceWeight()));
        product.setStatus(resolveStatus(request.getStatus(), true));

        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductStatusResponse updateProductStatus(String id, ProductStatusUpdateRequest request) {
        ProductEntity product = findProduct(id);
        product.setStatus(resolveStatus(request.getStatus(), true));
        return productMapper.toStatusResponse(productRepository.save(product));
    }

    private ProductEntity findProduct(String id) {
        return productRepository.findById(id).orElseThrow(ProductNotFoundException::new);
    }

    private void assertUniqueProductCode(String productCode, String currentProductId) {
        Optional<ProductEntity> existingProduct = productRepository.findByProductCodeIgnoreCase(productCode);
        if (existingProduct.isPresent() && !existingProduct.get().getId().equals(currentProductId)) {
            throw new DuplicateProductCodeException();
        }
    }

    private void normalizeAndValidateQuery(ProductListQuery query) {
        query.setKeyword(normalizeNullableText(query.getKeyword()));
        query.setType(normalizeNullableText(query.getType()));
        query.setSize(normalizeNullableText(query.getSize()));
        query.setThickness(normalizeNullableText(query.getThickness()));
        query.setUnit(normalizeNullableText(query.getUnit()));
        query.setStatus(normalizeNullableText(query.getStatus()));
        query.setSortBy(StringUtils.hasText(query.getSortBy()) ? query.getSortBy().trim() : "createdAt");
        query.setSortDir(StringUtils.hasText(query.getSortDir()) ? query.getSortDir().trim().toLowerCase(Locale.ROOT) : "desc");

        if (StringUtils.hasText(query.getStatus())) {
            query.setStatus(resolveStatus(query.getStatus(), true));
        }
        if (!ALLOWED_SORT_FIELDS.contains(query.getSortBy())) {
            throw RequestValidationException.singleError("sortBy", "sortBy must be one of createdAt, productCode, productName");
        }
        if (!ALLOWED_SORT_DIRECTIONS.contains(query.getSortDir())) {
            throw RequestValidationException.singleError("sortDir", "sortDir must be asc or desc");
        }
    }

    private Sort buildSort(ProductListQuery query) {
        Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, query.getSortBy());
    }

    private String resolveStatus(String status, boolean required) {
        if (!StringUtils.hasText(status)) {
            if (required) {
                throw RequestValidationException.singleError("status", "status must be ACTIVE or INACTIVE");
            }
            return ProductStatus.ACTIVE.name();
        }

        try {
            return ProductStatus.from(status).name();
        } catch (IllegalArgumentException exception) {
            throw RequestValidationException.singleError("status", "status must be ACTIVE or INACTIVE");
        }
    }

    private BigDecimal normalizeDecimal(BigDecimal value) {
        return value == null ? null : value.setScale(4, RoundingMode.HALF_UP);
    }

    private String normalizeText(String value) {
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
