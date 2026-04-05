package com.g90.backend.security;

import com.g90.backend.modules.account.entity.AccessTokenEntity;
import com.g90.backend.modules.account.repository.AccessTokenRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AccessTokenService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long TOKEN_VALIDITY_HOURS = 12L;
    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenRepository accessTokenRepository;

    public String issueToken(String userId) {
        String token = generateToken();

        AccessTokenEntity accessToken = new AccessTokenEntity();
        accessToken.setToken(token);
        accessToken.setUserId(userId);
        accessToken.setExpiresAt(LocalDateTime.now(APP_ZONE).plusHours(TOKEN_VALIDITY_HOURS));
        accessTokenRepository.save(accessToken);

        return token;
    }

    public String resolveUserId(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }

        return accessTokenRepository.findById(token)
                .map(accessToken -> {
                    if (accessToken.getExpiresAt().isBefore(LocalDateTime.now(APP_ZONE))) {
                        accessTokenRepository.delete(accessToken);
                        return null;
                    }
                    return accessToken.getUserId();
                })
                .orElse(null);
    }

    public void invalidate(String token) {
        if (StringUtils.hasText(token)) {
            accessTokenRepository.deleteById(token);
        }
    }

    public void invalidateAll(String userId) {
        if (StringUtils.hasText(userId)) {
            accessTokenRepository.deleteByUserId(userId);
        }
    }

    public String extractBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            return null;
        }

        String headerValue = authorizationHeader.trim();
        if (headerValue.length() < BEARER_PREFIX.length()
                || !headerValue.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }

        String token = headerValue.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? token : null;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }
}
