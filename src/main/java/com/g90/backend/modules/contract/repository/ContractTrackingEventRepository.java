package com.g90.backend.modules.contract.repository;

import com.g90.backend.modules.contract.entity.ContractTrackingEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractTrackingEventRepository extends JpaRepository<ContractTrackingEventEntity, String> {

    List<ContractTrackingEventEntity> findByContract_IdOrderByActualAtAscCreatedAtAsc(String contractId);
}
