package com.g90.backend.modules.inventory.repository;

import com.g90.backend.modules.inventory.entity.InventoryTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransactionEntity, String>,
        JpaSpecificationExecutor<InventoryTransactionEntity> {
}
