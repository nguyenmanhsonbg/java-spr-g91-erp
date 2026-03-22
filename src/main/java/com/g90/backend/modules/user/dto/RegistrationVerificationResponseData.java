package com.g90.backend.modules.user.dto;

import lombok.Builder;

@Builder
public record RegistrationVerificationResponseData(
        String userId,
        String email,
        String status,
        boolean verified,
        String redirectTo
) {
}
