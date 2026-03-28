package com.g90.backend.modules.promotion.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record PromotionDetailResponse(
        String id,
        String code,
        String name,
        String promotionType,
        BigDecimal discountValue,
        LocalDate validFrom,
        LocalDate validTo,
        String status,
        Integer priority,
        String description,
        String createdBy,
        String updatedBy,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt,
        List<PromotionScopeProductResponse> products,
        List<String> customerGroups
) {
}
