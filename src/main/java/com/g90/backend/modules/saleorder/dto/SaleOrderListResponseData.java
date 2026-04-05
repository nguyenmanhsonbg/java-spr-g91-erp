package com.g90.backend.modules.saleorder.dto;

import com.g90.backend.modules.product.dto.PaginationResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SaleOrderListResponseData(
        List<Item> items,
        PaginationResponse pagination,
        Filters filters
) {
    public record Item(
            String id,
            String saleOrderNumber,
            String contractNumber,
            String customerId,
            String customerName,
            String projectId,
            String projectCode,
            String projectName,
            LocalDate orderDate,
            LocalDate expectedDeliveryDate,
            LocalDate actualDeliveryDate,
            String status,
            BigDecimal totalAmount
    ) {
    }

    public record Filters(
            String keyword,
            String saleOrderNumber,
            String contractNumber,
            String customerId,
            String projectId,
            String status,
            LocalDate orderFrom,
            LocalDate orderTo,
            LocalDate deliveryFrom,
            LocalDate deliveryTo
    ) {
    }
}
