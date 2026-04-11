package com.g90.backend.modules.contract.dto;

import com.g90.backend.modules.payment.dto.PaymentOptionData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ContractFormInitResponseData(
        CustomerData customer,
        QuotationData quotation,
        DefaultsData defaults,
        List<ItemData> items,
        List<String> warnings,
        List<PaymentOptionData> availablePaymentOptions
) {
    public record CustomerData(
            String id,
            String companyName,
            String customerType,
            String contactPerson,
            String phone,
            String email,
            String address,
            BigDecimal creditLimit,
            BigDecimal currentDebt,
            BigDecimal availableCredit,
            BigDecimal depositPercentage
    ) {
    }

    public record QuotationData(
            String id,
            String quotationNumber,
            String status,
            LocalDate validUntil,
            String projectId,
            String projectCode,
            String projectName,
            String deliveryRequirements,
            String note,
            PaymentOptionData paymentOption
    ) {
    }

    public record DefaultsData(
            String suggestedPaymentTerms,
            String suggestedDeliveryAddress,
            String suggestedDeliveryTerms,
            PaymentOptionData suggestedPaymentOption
    ) {
    }

    public record ItemData(
            String productId,
            String productCode,
            String productName,
            String unit,
            BigDecimal quantity,
            BigDecimal quotedUnitPrice,
            BigDecimal baseUnitPrice,
            BigDecimal suggestedUnitPrice,
            BigDecimal totalPrice
    ) {
    }
}
