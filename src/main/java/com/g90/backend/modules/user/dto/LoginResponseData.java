package com.g90.backend.modules.user.dto;

import lombok.Builder;

@Builder
public record LoginResponseData(
        String accessToken,
        String tokenType,
        AuthenticatedUserResponse user
) {
}
