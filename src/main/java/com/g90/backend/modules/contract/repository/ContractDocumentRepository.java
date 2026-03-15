package com.g90.backend.modules.contract.repository;

import com.g90.backend.modules.contract.entity.ContractDocumentEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractDocumentRepository extends JpaRepository<ContractDocumentEntity, String> {

    List<ContractDocumentEntity> findByContract_IdOrderByGeneratedAtDesc(String contractId);

    long countByDocumentTypeAndOfficialDocumentTrueAndGeneratedAtBetween(
            String documentType,
            LocalDateTime start,
            LocalDateTime end
    );
}
