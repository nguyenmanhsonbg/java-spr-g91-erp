package com.g90.backend.modules.quotation.mapper;

import com.g90.backend.modules.payment.dto.PaymentOptionData;
import com.g90.backend.modules.payment.entity.PaymentOptionEntity;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.quotation.dto.QuotationItemResponse;
import com.g90.backend.modules.quotation.dto.QuotationSaveResponseData;
import com.g90.backend.modules.quotation.dto.QuotationSubmitResponseData;
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

    public List<QuotationItemResponse> toItemResponses(List<QuotationItemEntity> items) {
        return items.stream().map(this::toItemResponse).toList();
    }

    public QuotationSaveResponseData toSaveResponse(QuotationEntity entity) {
        return new QuotationSaveResponseData(
                new QuotationSaveResponseData.QuotationData(
                        entity.getId(),
                        entity.getQuotationNumber(),
                        entity.getCustomer().getId(),
                        entity.getProject() == null ? null : entity.getProject().getId(),
                        entity.getTotalAmount(),
                        entity.getStatus(),
                        entity.getValidUntil(),
                        entity.getCreatedAt()
                ),
                toItemResponses(entity.getItems()),
                new QuotationSaveResponseData.MetadataData(
                        entity.getDeliveryRequirement(),
                        entity.getPromotionCode(),
                        toPaymentOptionData(entity.getPaymentOption())
                )
        );
    }

    public QuotationSubmitResponseData toSubmitResponse(QuotationEntity entity, String nextAction) {
        return new QuotationSubmitResponseData(
                new QuotationSubmitResponseData.QuotationData(
                        entity.getId(),
                        entity.getQuotationNumber(),
                        entity.getCustomer().getId(),
                        entity.getProject() == null ? null : entity.getProject().getId(),
                        entity.getTotalAmount(),
                        entity.getStatus(),
                        toPaymentOptionData(entity.getPaymentOption()),
                        entity.getValidUntil(),
                        entity.getCreatedAt()
                ),
                new QuotationSubmitResponseData.TrackingData(
                        entity.getSubmittedAt(),
                        nextAction
                )
        );
    }

    public PaymentOptionData toPaymentOptionData(PaymentOptionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PaymentOptionData(entity.getCode(), entity.getName(), entity.getDescription());
    }
}
