package com.g90.backend.modules.account.repository;

import com.g90.backend.modules.account.entity.RoleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, String> {

    Optional<RoleEntity> findByNameIgnoreCase(String name);
}
