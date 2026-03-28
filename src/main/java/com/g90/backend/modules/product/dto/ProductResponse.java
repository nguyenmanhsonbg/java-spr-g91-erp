package com.g90.backend.modules.product.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record ProductResponse(
        String id,
        String productCode,
        String productName,
        String type,
        String size,
        String thickness,
        String unit,
        BigDecimal weightConversion,
        BigDecimal referenceWeight,
        String description,
        List<String> imageUrls,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime updatedAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime deletedAt
) {
}
