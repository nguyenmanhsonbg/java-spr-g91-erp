package com.g90.backend.modules.account.repository;

import com.g90.backend.modules.account.entity.UserAccountEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, String>, JpaSpecificationExecutor<UserAccountEntity> {

    Optional<UserAccountEntity> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "role")
    Optional<UserAccountEntity> findWithRoleById(String id);

    @EntityGraph(attributePaths = "role")
    Optional<UserAccountEntity> findWithRoleByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "role")
    List<UserAccountEntity> findByIdIn(Collection<String> ids);

    @EntityGraph(attributePaths = "role")
    List<UserAccountEntity> findByRole_NameIgnoreCaseAndStatusIgnoreCase(String roleName, String status);
}
