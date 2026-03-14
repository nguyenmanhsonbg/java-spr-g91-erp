package com.g90.backend.security;

public record AuthenticatedUser(
        String userId,
        String email,
        String role,
        String accessToken
) {
}
