package com.g90.backend.modules.debt.repository;

import com.g90.backend.modules.debt.entity.DebtSettlementEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtSettlementRepository extends JpaRepository<DebtSettlementEntity, String> {

    List<DebtSettlementEntity> findByCustomerIdOrderBySettlementDateDescCreatedAtDesc(String customerId);
}
