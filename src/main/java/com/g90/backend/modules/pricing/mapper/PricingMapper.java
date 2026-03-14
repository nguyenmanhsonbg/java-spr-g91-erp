package com.g90.backend.modules.pricing.mapper;

import com.g90.backend.modules.pricing.dto.PriceListCreateDataResponse;
import com.g90.backend.modules.pricing.dto.PriceListCreateRequest;
import com.g90.backend.modules.pricing.dto.PriceListDetailResponse;
import com.g90.backend.modules.pricing.dto.PriceListItemCreateDataResponse;
import com.g90.backend.modules.pricing.dto.PriceListItemResponse;
import com.g90.backend.modules.pricing.dto.PriceListListItemResponse;
import com.g90.backend.modules.pricing.dto.PriceListListResponseData;
import com.g90.backend.modules.pricing.dto.PriceListUpdateRequest;
import com.g90.backend.modules.pricing.entity.PriceListEntity;
import com.g90.backend.modules.pricing.entity.PriceListItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PricingMapper {

    public PriceListEntity toEntity(PriceListCreateRequest request) {
        PriceListEntity entity = new PriceListEntity();
        entity.setName(request.getName());
        entity.setCustomerGroup(request.getCustomerGroup());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setStatus(request.getStatus());
        return entity;
    }

    public void updateEntity(PriceListEntity entity, PriceListUpdateRequest request) {
        entity.setName(request.getName());
        entity.setCustomerGroup(request.getCustomerGroup());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setStatus(request.getStatus());
    }

    public PriceListCreateDataResponse toCreateData(PriceListEntity entity) {
        return PriceListCreateDataResponse.builder()
                .id(entity.getId())
                .build();
    }

    public PriceListItemCreateDataResponse toItemCreateData(PriceListItemEntity entity) {
        return PriceListItemCreateDataResponse.builder()
                .id(entity.getId())
                .build();
    }

    public PriceListListItemResponse toListItem(PriceListEntity entity) {
        return PriceListListItemResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .customerGroup(entity.getCustomerGroup())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public PriceListListResponseData toListResponse(Page<PriceListEntity> page) {
        return PriceListListResponseData.builder()
                .content(page.getContent().stream().map(this::toListItem).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .build();
    }

    public PriceListDetailResponse toDetailResponse(PriceListEntity entity) {
        return PriceListDetailResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .customerGroup(entity.getCustomerGroup())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus())
                .items(entity.getItems().stream().map(this::toItemResponse).toList())
                .build();
    }

    public PriceListItemResponse toItemResponse(PriceListItemEntity entity) {
        return PriceListItemResponse.builder()
                .id(entity.getId())
                .productId(entity.getProduct().getId())
                .productName(entity.getProduct().getProductName())
                .unitPrice(entity.getUnitPrice())
                .build();
    }
}
