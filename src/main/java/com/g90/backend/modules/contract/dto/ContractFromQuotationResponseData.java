package com.g90.backend.modules.contract.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContractFromQuotationResponseData(
        ContractData contract,
        QuotationData quotation
) {

    public record ContractData(
            String id,
            String contractNumber,
            String customerId,
            String quotationId,
            BigDecimal totalAmount,
            String status,
            String paymentTerms,
            String deliveryAddress,
            LocalDateTime createdAt
    ) {
    }

    public record QuotationData(
            String id,
            String quotationNumber,
            String status
    ) {
    }
}
