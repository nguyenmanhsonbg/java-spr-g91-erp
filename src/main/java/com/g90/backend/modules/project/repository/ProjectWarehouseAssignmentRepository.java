package com.g90.backend.modules.project.repository;

import com.g90.backend.modules.project.entity.ProjectWarehouseAssignmentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectWarehouseAssignmentRepository extends JpaRepository<ProjectWarehouseAssignmentEntity, String> {

    List<ProjectWarehouseAssignmentEntity> findByProject_IdOrderByAssignedAtDesc(String projectId);

    List<ProjectWarehouseAssignmentEntity> findByProject_IdAndActiveTrue(String projectId);
}
