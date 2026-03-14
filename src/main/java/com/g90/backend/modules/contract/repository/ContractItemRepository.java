package com.g90.backend.modules.contract.repository;

import com.g90.backend.modules.contract.entity.ContractItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractItemRepository extends JpaRepository<ContractItemEntity, String> {
}
