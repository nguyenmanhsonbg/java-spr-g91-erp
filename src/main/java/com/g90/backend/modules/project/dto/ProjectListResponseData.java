package com.g90.backend.modules.project.dto;

import com.g90.backend.modules.product.dto.PaginationResponse;
import java.time.LocalDate;
import java.util.List;

public record ProjectListResponseData(
        List<ProjectResponse> items,
        PaginationResponse pagination,
        Filters filters
) {

    public record Filters(
            String projectCode,
            String projectName,
            String customerId,
            String status,
            String progressStatus,
            String warehouseId,
            String assignedManager,
            Boolean archived,
            LocalDate createdFrom,
            LocalDate createdTo,
            LocalDate startFrom,
            LocalDate startTo,
            LocalDate endFrom,
            LocalDate endTo
    ) {
    }
}
