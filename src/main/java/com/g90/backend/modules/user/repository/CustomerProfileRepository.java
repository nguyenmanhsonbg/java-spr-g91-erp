package com.g90.backend.modules.user.repository;

import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfileEntity, String> {

    Optional<CustomerProfileEntity> findByUser_Id(String userId);
}
