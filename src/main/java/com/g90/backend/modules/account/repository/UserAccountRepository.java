package com.g90.backend.modules.account.repository;

import com.g90.backend.modules.account.entity.UserAccountEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, String>, JpaSpecificationExecutor<UserAccountEntity> {

    Optional<UserAccountEntity> findByEmailIgnoreCase(String email);
}
