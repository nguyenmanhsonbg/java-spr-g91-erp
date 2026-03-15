package com.g90.backend.modules.project.repository;

import com.g90.backend.modules.project.entity.ProjectInvoiceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectInvoiceRepository extends JpaRepository<ProjectInvoiceEntity, String> {

    List<ProjectInvoiceEntity> findByContractId(String contractId);
}
