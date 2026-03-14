package com.g90.backend.modules.user.repository;

import com.g90.backend.modules.user.entity.PasswordResetTokenEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, String> {

    Optional<PasswordResetTokenEntity> findByTokenAndUsedFalse(String token);

    List<PasswordResetTokenEntity> findByUser_IdAndUsedFalse(String userId);
}
