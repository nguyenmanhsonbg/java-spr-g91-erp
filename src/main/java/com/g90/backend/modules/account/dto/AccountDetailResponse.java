package com.g90.backend.modules.account.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AccountDetailResponse(
        String id,
        String fullName,
        String email,
        String phone,
        String address,
        String role,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt
) {
}
