package com.g90.backend.modules.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "access_tokens")
public class AccessTokenEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "token", length = 128, nullable = false)
    private String token;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(APP_ZONE);
        }
    }
}
