package com.g90.backend.modules.contract.repository;

import com.g90.backend.modules.contract.entity.ContractVersionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractVersionRepository extends JpaRepository<ContractVersionEntity, String> {

    List<ContractVersionEntity> findByContract_IdOrderByVersionNoDesc(String contractId);

    long countByContract_Id(String contractId);
}
