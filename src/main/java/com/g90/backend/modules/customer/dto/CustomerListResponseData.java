package com.g90.backend.modules.customer.dto;

import com.g90.backend.modules.product.dto.PaginationResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerListResponseData(
        List<Item> items,
        PaginationResponse pagination,
        Filters filters
) {
    public record Item(
            String id,
            String customerCode,
            String companyName,
            String taxCode,
            String contactPerson,
            String phone,
            String email,
            String customerType,
            String priceGroup,
            BigDecimal creditLimit,
            String status,
            boolean portalAccountLinked,
            LocalDateTime createdAt
    ) {
    }

    public record Filters(
            String keyword,
            String customerCode,
            String taxCode,
            String customerType,
            String priceGroup,
            String status,
            LocalDate createdFrom,
            LocalDate createdTo
    ) {
    }
}
