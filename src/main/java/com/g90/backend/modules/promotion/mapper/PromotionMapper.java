package com.g90.backend.modules.promotion.mapper;

import com.g90.backend.modules.product.dto.PaginationResponse;
import com.g90.backend.modules.promotion.dto.PromotionCreateDataResponse;
import com.g90.backend.modules.promotion.dto.PromotionDetailResponse;
import com.g90.backend.modules.promotion.dto.PromotionListItemResponse;
import com.g90.backend.modules.promotion.dto.PromotionListQuery;
import com.g90.backend.modules.promotion.dto.PromotionListResponseData;
import com.g90.backend.modules.promotion.dto.PromotionScopeProductResponse;
import com.g90.backend.modules.promotion.entity.PromotionCustomerGroupEntity;
import com.g90.backend.modules.promotion.entity.PromotionEntity;
import com.g90.backend.modules.promotion.entity.PromotionProductEntity;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PromotionMapper {

    public PromotionCreateDataResponse toCreateData(PromotionEntity entity) {
        return PromotionCreateDataResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .build();
    }

    public PromotionListItemResponse toListItem(PromotionEntity entity) {
        List<PromotionProductEntity> products = activeProducts(entity);
        List<String> customerGroups = activeCustomerGroups(entity);
        return PromotionListItemResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .promotionType(entity.getPromotionType())
                .discountValue(entity.getDiscountValue())
                .validFrom(entity.getStartDate())
                .validTo(entity.getEndDate())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .scopeSummary(scopeSummary(products.size(), customerGroups))
                .productCount(products.size())
                .customerGroups(customerGroups)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PromotionListResponseData toListResponse(Page<PromotionEntity> page, PromotionListQuery query) {
        return new PromotionListResponseData(
                page.getContent().stream().map(this::toListItem).toList(),
                PaginationResponse.builder()
                        .page(page.getNumber() + 1)
                        .pageSize(page.getSize())
                        .totalItems(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build(),
                new PromotionListResponseData.Filters(
                        query.getSearch(),
                        query.getStatus(),
                        query.getPromotionType(),
                        query.getValidFrom(),
                        query.getValidTo(),
                        query.getCustomerGroup(),
                        query.getProductId()
                )
        );
    }

    public PromotionDetailResponse toDetailResponse(PromotionEntity entity) {
        return PromotionDetailResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .promotionType(entity.getPromotionType())
                .discountValue(entity.getDiscountValue())
                .validFrom(entity.getStartDate())
                .validTo(entity.getEndDate())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .description(entity.getDescription())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .products(activeProducts(entity).stream()
                        .sorted(Comparator.comparing(item -> item.getProduct() == null ? "" : item.getProduct().getProductCode(), String.CASE_INSENSITIVE_ORDER))
                        .map(item -> PromotionScopeProductResponse.builder()
                                .productId(item.getProduct() == null ? null : item.getProduct().getId())
                                .productCode(item.getProduct() == null ? null : item.getProduct().getProductCode())
                                .productName(item.getProduct() == null ? null : item.getProduct().getProductName())
                                .build())
                        .toList())
                .customerGroups(activeCustomerGroups(entity))
                .build();
    }

    private String scopeSummary(int productCount, List<String> customerGroups) {
        String productSummary = productCount == 0 ? "All products" : productCount + " product(s)";
        String customerSummary = customerGroups.isEmpty() ? "All customer groups" : String.join(", ", customerGroups);
        return productSummary + " | " + customerSummary;
    }

    private List<PromotionProductEntity> activeProducts(PromotionEntity entity) {
        return entity.getProducts().stream()
                .filter(item -> item.getDeletedAt() == null)
                .toList();
    }

    private List<String> activeCustomerGroups(PromotionEntity entity) {
        return entity.getCustomerGroups().stream()
                .filter(item -> item.getDeletedAt() == null)
                .map(PromotionCustomerGroupEntity::getCustomerGroup)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
