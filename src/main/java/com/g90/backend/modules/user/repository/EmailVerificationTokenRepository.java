package com.g90.backend.modules.user.repository;

import com.g90.backend.modules.user.entity.EmailVerificationTokenEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationTokenEntity, String> {

    List<EmailVerificationTokenEntity> findByUser_IdAndConsumedAtIsNullAndInvalidatedAtIsNull(String userId);

    Optional<EmailVerificationTokenEntity> findFirstByUser_IdAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(String userId);

    Optional<EmailVerificationTokenEntity> findFirstByUser_IdOrderByCreatedAtDesc(String userId);
}
