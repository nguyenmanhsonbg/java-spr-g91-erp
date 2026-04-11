package com.g90.backend.modules.user.dto;

import java.time.LocalDateTime;

public record PasswordResetTokenValidationResponseData(
        boolean valid,
        LocalDateTime expiredAt
) {
}
