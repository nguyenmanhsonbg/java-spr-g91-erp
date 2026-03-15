package com.g90.backend.modules.contract.repository;

import com.g90.backend.modules.contract.entity.ContractApprovalEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractApprovalRepository extends JpaRepository<ContractApprovalEntity, String> {

    List<ContractApprovalEntity> findByContract_IdOrderByRequestedAtDesc(String contractId);

    List<ContractApprovalEntity> findByStatusOrderByRequestedAtAsc(String status);
}
