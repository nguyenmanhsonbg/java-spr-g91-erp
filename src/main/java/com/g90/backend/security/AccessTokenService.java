package com.g90.backend.security;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AccessTokenService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long TOKEN_VALIDITY_HOURS = 12L;

    private final Map<String, TokenSession> sessions = new ConcurrentHashMap<>();

    public String issueToken(String userId) {
        String token = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new TokenSession(userId, LocalDateTime.now(APP_ZONE).plusHours(TOKEN_VALIDITY_HOURS)));
        return token;
    }

    public String resolveUserId(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }

        TokenSession session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.expiresAt().isBefore(LocalDateTime.now(APP_ZONE))) {
            sessions.remove(token);
            return null;
        }
        return session.userId();
    }

    public void invalidate(String token) {
        if (StringUtils.hasText(token)) {
            sessions.remove(token);
        }
    }

    public void invalidateAll(String userId) {
        sessions.entrySet().removeIf(entry -> entry.getValue().userId().equals(userId));
    }

    public String extractBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authorizationHeader.substring(7).trim();
        return StringUtils.hasText(token) ? token : null;
    }

    private record TokenSession(String userId, LocalDateTime expiresAt) {
    }
}
