package com.g90.backend.modules.customer.dto;

import java.time.LocalDateTime;

public record CustomerStatusResponse(
        String id,
        String customerCode,
        String status,
        String reason,
        LocalDateTime updatedAt
) {
}
