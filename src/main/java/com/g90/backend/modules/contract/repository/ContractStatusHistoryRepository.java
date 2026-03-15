package com.g90.backend.modules.contract.repository;

import com.g90.backend.modules.contract.entity.ContractStatusHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractStatusHistoryRepository extends JpaRepository<ContractStatusHistoryEntity, String> {

    List<ContractStatusHistoryEntity> findByContract_IdOrderByChangedAtAsc(String contractId);
}
