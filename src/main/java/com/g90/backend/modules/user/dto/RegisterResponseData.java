package com.g90.backend.modules.user.dto;

import lombok.Builder;

@Builder
public record RegisterResponseData(
        String userId,
        String redirectTo
) {
}
