package com.g90.backend.modules.pricing.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record PriceListDetailResponse(
        String id,
        String name,
        String customerGroup,
        LocalDate validFrom,
        LocalDate validTo,
        String status,
        String createdBy,
        String updatedBy,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt,
        List<PriceListItemResponse> items
) {
}
