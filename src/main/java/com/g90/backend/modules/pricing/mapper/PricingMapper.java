package com.g90.backend.modules.pricing.mapper;

import com.g90.backend.modules.pricing.dto.PriceListCreateDataResponse;
import com.g90.backend.modules.pricing.dto.PriceListDetailResponse;
import com.g90.backend.modules.pricing.dto.PriceListItemResponse;
import com.g90.backend.modules.pricing.dto.PriceListListItemResponse;
import com.g90.backend.modules.pricing.dto.PriceListListQuery;
import com.g90.backend.modules.pricing.dto.PriceListListResponseData;
import com.g90.backend.modules.pricing.entity.PriceListEntity;
import com.g90.backend.modules.pricing.entity.PriceListItemEntity;
import com.g90.backend.modules.product.dto.PaginationResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PricingMapper {

    public PriceListCreateDataResponse toCreateData(PriceListEntity entity) {
        return PriceListCreateDataResponse.builder()
                .id(entity.getId())
                .build();
    }

    public PriceListListItemResponse toListItem(PriceListEntity entity) {
        return PriceListListItemResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .customerGroup(entity.getCustomerGroup())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .status(entity.getStatus())
                .itemCount(activeItems(entity).size())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PriceListListResponseData toListResponse(Page<PriceListEntity> page, PriceListListQuery query) {
        return new PriceListListResponseData(
                page.getContent().stream().map(this::toListItem).toList(),
                PaginationResponse.builder()
                        .page(page.getNumber() + 1)
                        .pageSize(page.getSize())
                        .totalItems(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build(),
                new PriceListListResponseData.Filters(
                        query.getSearch(),
                        query.getStatus(),
                        query.getCustomerGroup(),
                        query.getValidFrom(),
                        query.getValidTo()
                )
        );
    }

    public PriceListDetailResponse toDetailResponse(PriceListEntity entity) {
        return PriceListDetailResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .customerGroup(entity.getCustomerGroup())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .items(activeItems(entity).stream()
                        .sorted(Comparator.comparing(item -> item.getProduct() == null ? "" : item.getProduct().getProductCode(), String.CASE_INSENSITIVE_ORDER))
                        .map(this::toItemResponse)
                        .toList())
                .build();
    }

    public PriceListItemResponse toItemResponse(PriceListItemEntity entity) {
        return PriceListItemResponse.builder()
                .id(entity.getId())
                .productId(entity.getProduct() == null ? null : entity.getProduct().getId())
                .productCode(entity.getProduct() == null ? null : entity.getProduct().getProductCode())
                .productName(entity.getProduct() == null ? null : entity.getProduct().getProductName())
                .unitPriceVnd(entity.getUnitPrice())
                .pricingRuleType(entity.getPricingRuleType())
                .note(entity.getNote())
                .build();
    }

    private List<PriceListItemEntity> activeItems(PriceListEntity entity) {
        return entity.getItems().stream()
                .filter(item -> item.getDeletedAt() == null)
                .toList();
    }
}
