package com.g90.backend.modules.quotation.dto;

import com.g90.backend.modules.product.dto.PaginationResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerQuotationListResponseData(
        List<ItemData> items,
        PaginationResponse pagination,
        FilterData filters
) {

    public record ItemData(
            String id,
            String quotationNumber,
            LocalDateTime createdAt,
            BigDecimal totalAmount,
            String status,
            LocalDate validUntil,
            ActionData actions
    ) {
    }

    public record ActionData(
            boolean canView,
            boolean canEdit,
            boolean canTrack
    ) {
    }

    public record FilterData(
            String status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
    }
}
