package com.g90.backend.modules.pricing.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PriceListListItemResponse(
        String id,
        String name,
        String customerGroup,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt
) {
}
