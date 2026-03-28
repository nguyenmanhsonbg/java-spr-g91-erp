package com.g90.backend.modules.product.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record ProductStatusResponse(
        String id,
        String productCode,
        String productName,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime updatedAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime deletedAt
) {
}
