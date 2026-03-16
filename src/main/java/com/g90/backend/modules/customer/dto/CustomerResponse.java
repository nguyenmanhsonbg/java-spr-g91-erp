package com.g90.backend.modules.customer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerResponse(
        String id,
        String customerCode,
        String companyName,
        String taxCode,
        String address,
        String contactPerson,
        String phone,
        String email,
        String customerType,
        String priceGroup,
        BigDecimal creditLimit,
        String paymentTerms,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
