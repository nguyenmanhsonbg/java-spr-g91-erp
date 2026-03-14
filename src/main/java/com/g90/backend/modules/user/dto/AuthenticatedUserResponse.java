package com.g90.backend.modules.user.dto;

import lombok.Builder;

@Builder
public record AuthenticatedUserResponse(
        String id,
        String fullName,
        String email,
        String role,
        String status
) {
}
