package com.g90.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.g90.backend.modules.account.entity.AccessTokenEntity;
import com.g90.backend.modules.account.repository.AccessTokenRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessTokenServiceTest {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private AccessTokenRepository accessTokenRepository;

    private AccessTokenService accessTokenService;

    @BeforeEach
    void setUp() {
        accessTokenService = new AccessTokenService(accessTokenRepository);
    }

    @Test
    void issueTokenPersistsTokenSession() {
        when(accessTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDateTime issuedAtFloor = LocalDateTime.now(APP_ZONE);

        String token = accessTokenService.issueToken("user-1");

        ArgumentCaptor<AccessTokenEntity> entityCaptor = ArgumentCaptor.forClass(AccessTokenEntity.class);
        verify(accessTokenRepository).save(entityCaptor.capture());
        assertThat(token).isEqualTo(entityCaptor.getValue().getToken());
        assertThat(entityCaptor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(entityCaptor.getValue().getExpiresAt()).isAfter(issuedAtFloor.plusHours(11));
    }

    @Test
    void resolveUserIdReturnsNullAndDeletesExpiredToken() {
        AccessTokenEntity entity = new AccessTokenEntity();
        entity.setToken("token");
        entity.setUserId("user-1");
        entity.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(accessTokenRepository.findById("token")).thenReturn(Optional.of(entity));

        String userId = accessTokenService.resolveUserId("token");

        assertThat(userId).isNull();
        verify(accessTokenRepository).delete(entity);
    }

    @Test
    void resolveUserIdReturnsUserIdForActiveToken() {
        AccessTokenEntity entity = new AccessTokenEntity();
        entity.setToken("token");
        entity.setUserId("user-1");
        entity.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(accessTokenRepository.findById("token")).thenReturn(Optional.of(entity));

        assertThat(accessTokenService.resolveUserId("token")).isEqualTo("user-1");
    }

    @Test
    void invalidateAllDeletesByUserId() {
        accessTokenService.invalidateAll("user-1");

        verify(accessTokenRepository).deleteByUserId("user-1");
    }

    @Test
    void extractBearerTokenIsCaseInsensitive() {
        assertThat(accessTokenService.extractBearerToken("Bearer abc123")).isEqualTo("abc123");
        assertThat(accessTokenService.extractBearerToken("bearer abc123")).isEqualTo("abc123");
        assertThat(accessTokenService.extractBearerToken("BEARER abc123")).isEqualTo("abc123");
        assertThat(accessTokenService.extractBearerToken("Token abc123")).isNull();
    }
}
