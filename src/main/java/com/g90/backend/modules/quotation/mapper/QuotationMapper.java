package com.g90.backend.modules.quotation.mapper;

import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.quotation.dto.QuotationItemResponse;
import com.g90.backend.modules.quotation.dto.QuotationPreviewResponseData;
import com.g90.backend.modules.quotation.dto.QuotationResponseData;
import com.g90.backend.modules.quotation.entity.QuotationEntity;
import com.g90.backend.modules.quotation.entity.QuotationItemEntity;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QuotationMapper {

    public QuotationItemResponse toItemResponse(
            String id,
            ProductEntity product,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {
        return QuotationItemResponse.builder()
                .id(id)
                .productId(product.getId())
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .type(product.getType())
                .size(product.getSize())
                .thickness(product.getThickness())
                .unit(product.getUnit())
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .build();
    }

    public QuotationItemResponse toItemResponse(QuotationItemEntity entity) {
        return toItemResponse(
                entity.getId(),
                entity.getProduct(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getTotalPrice()
        );
    }

    public QuotationPreviewResponseData toPreviewResponse(
            String customerId,
            String projectId,
            String status,
            java.time.LocalDate validUntil,
            BigDecimal totalAmount,
            String note,
            String deliveryRequirement,
            List<QuotationItemResponse> items
    ) {
        return QuotationPreviewResponseData.builder()
                .customerId(customerId)
                .projectId(projectId)
                .status(status)
                .validUntil(validUntil)
                .totalAmount(totalAmount)
                .note(note)
                .deliveryRequirement(deliveryRequirement)
                .items(items)
                .build();
    }

    public QuotationResponseData toResponse(QuotationEntity entity) {
        return QuotationResponseData.builder()
                .id(entity.getId())
                .quotationNumber(entity.getQuotationNumber())
                .customerId(entity.getCustomer().getId())
                .projectId(entity.getProject() == null ? null : entity.getProject().getId())
                .status(entity.getStatus())
                .validUntil(entity.getValidUntil())
                .totalAmount(entity.getTotalAmount())
                .note(entity.getNote())
                .deliveryRequirement(entity.getDeliveryRequirement())
                .items(entity.getItems().stream().map(this::toItemResponse).toList())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
