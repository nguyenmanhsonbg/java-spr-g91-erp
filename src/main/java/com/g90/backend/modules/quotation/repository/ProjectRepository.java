package com.g90.backend.modules.quotation.repository;

import com.g90.backend.modules.quotation.entity.ProjectEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, String> {

    Optional<ProjectEntity> findByIdAndCustomer_Id(String id, String customerId);
}
