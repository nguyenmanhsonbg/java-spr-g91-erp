package com.g90.backend.modules.product.mapper;

import com.g90.backend.modules.product.dto.PaginationResponse;
import com.g90.backend.modules.product.dto.ProductCreateRequest;
import com.g90.backend.modules.product.dto.ProductFiltersResponse;
import com.g90.backend.modules.product.dto.ProductListQuery;
import com.g90.backend.modules.product.dto.ProductListResponseData;
import com.g90.backend.modules.product.dto.ProductResponse;
import com.g90.backend.modules.product.dto.ProductStatusResponse;
import com.g90.backend.modules.product.dto.ProductUpdateRequest;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.entity.ProductImageEntity;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public ProductEntity toEntity(ProductCreateRequest request) {
        ProductEntity entity = new ProductEntity();
        entity.setProductCode(request.getProductCode());
        entity.setProductName(request.getProductName());
        entity.setType(request.getType());
        entity.setSize(request.getSize());
        entity.setThickness(request.getThickness());
        entity.setUnit(request.getUnit());
        entity.setWeightConversion(request.getWeightConversion());
        entity.setReferenceWeight(request.getReferenceWeight());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus());
        return entity;
    }

    public void updateEntity(ProductEntity entity, ProductUpdateRequest request) {
        entity.setProductCode(request.getProductCode());
        entity.setProductName(request.getProductName());
        entity.setType(request.getType());
        entity.setSize(request.getSize());
        entity.setThickness(request.getThickness());
        entity.setUnit(request.getUnit());
        entity.setWeightConversion(request.getWeightConversion());
        entity.setReferenceWeight(request.getReferenceWeight());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus());
    }

    public ProductResponse toResponse(ProductEntity entity) {
        return ProductResponse.builder()
                .id(entity.getId())
                .productCode(entity.getProductCode())
                .productName(entity.getProductName())
                .type(entity.getType())
                .size(entity.getSize())
                .thickness(entity.getThickness())
                .unit(entity.getUnit())
                .weightConversion(entity.getWeightConversion())
                .referenceWeight(entity.getReferenceWeight())
                .description(entity.getDescription())
                .imageUrls(toImageUrls(entity))
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().atZone(APP_ZONE).toOffsetDateTime())
                .updatedAt(entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().atZone(APP_ZONE).toOffsetDateTime())
                .deletedAt(entity.getDeletedAt() == null ? null : entity.getDeletedAt().atZone(APP_ZONE).toOffsetDateTime())
                .build();
    }

    public ProductStatusResponse toStatusResponse(ProductEntity entity) {
        return ProductStatusResponse.builder()
                .id(entity.getId())
                .productCode(entity.getProductCode())
                .productName(entity.getProductName())
                .status(entity.getStatus())
                .updatedAt(entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().atZone(APP_ZONE).toOffsetDateTime())
                .deletedAt(entity.getDeletedAt() == null ? null : entity.getDeletedAt().atZone(APP_ZONE).toOffsetDateTime())
                .build();
    }

    public ProductListResponseData toListResponse(Page<ProductEntity> page, ProductListQuery query) {
        return ProductListResponseData.builder()
                .items(page.getContent().stream().map(this::toResponse).toList())
                .pagination(PaginationResponse.builder()
                        .page(query.getPage())
                        .pageSize(query.getPageSize())
                        .totalItems(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .filters(ProductFiltersResponse.builder()
                        .keyword(query.getKeyword())
                        .search(query.getKeyword())
                        .type(query.getType())
                        .size(query.getSize())
                        .thickness(query.getThickness())
                        .unit(query.getUnit())
                        .status(query.getStatus())
                        .build())
                .build();
    }

    private List<String> toImageUrls(ProductEntity entity) {
        if (entity.getImages() == null || entity.getImages().isEmpty()) {
            return List.of();
        }

        return entity.getImages().stream()
                .map(ProductImageEntity::getImageUrl)
                .toList();
    }
}
