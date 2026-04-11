package com.g90.backend.modules.quotation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.g90.backend.modules.payment.dto.PaymentOptionData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record QuotationDetailResponseData(
        QuotationData quotation,
        CustomerData customer,
        ProjectData project,
        List<QuotationItemResponse> items,
        PricingData pricing,
        PaymentOptionData paymentOption,
        @JsonProperty("deliveryRequirements") String deliveryRequirements,
        ActionData actions
) {

    public record QuotationData(
            String id,
            String quotationNumber,
            String status,
            BigDecimal totalAmount,
            LocalDate validUntil,
            LocalDateTime createdAt
    ) {
    }

    public record CustomerData(
            String id,
            String companyName,
            String taxCode,
            String address,
            String contactPerson,
            String phone,
            String email,
            String customerType
    ) {
    }

    public record ProjectData(
            String id,
            String projectCode,
            String name,
            String location,
            String status
    ) {
    }

    public record PricingData(
            BigDecimal subTotal,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            String promotionCode
    ) {
    }

    public record ActionData(
            boolean customerCanEdit,
            boolean accountantCanCreateContract
    ) {
    }
}
