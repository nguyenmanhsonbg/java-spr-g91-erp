package com.g90.backend.modules.contract.repository;

import com.g90.backend.modules.contract.entity.ContractEntity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<ContractEntity, String> {

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    boolean existsByQuotation_Id(String quotationId);
}
