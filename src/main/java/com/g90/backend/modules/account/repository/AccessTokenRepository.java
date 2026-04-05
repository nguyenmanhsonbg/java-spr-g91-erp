package com.g90.backend.modules.account.repository;

import com.g90.backend.modules.account.entity.AccessTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessTokenRepository extends JpaRepository<AccessTokenEntity, String> {

    void deleteByUserId(String userId);
}
