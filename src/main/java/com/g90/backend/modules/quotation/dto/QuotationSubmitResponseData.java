package com.g90.backend.modules.quotation.dto;

import com.g90.backend.modules.payment.dto.PaymentOptionData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record QuotationSubmitResponseData(
        QuotationData quotation,
        TrackingData tracking
) {

    public record QuotationData(
            String id,
            String quotationNumber,
            String customerId,
            String projectId,
            BigDecimal totalAmount,
            String status,
            PaymentOptionData paymentOption,
            LocalDate validUntil,
            LocalDateTime createdAt
    ) {
    }

    public record TrackingData(
            LocalDateTime submittedAt,
            String nextAction
    ) {
    }
}
