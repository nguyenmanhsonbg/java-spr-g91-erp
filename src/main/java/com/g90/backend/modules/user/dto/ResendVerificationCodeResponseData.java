package com.g90.backend.modules.user.dto;

import lombok.Builder;

@Builder
public record ResendVerificationCodeResponseData(
        String userId,
        String email,
        int expireMinutes
) {
}
