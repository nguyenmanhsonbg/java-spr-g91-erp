package com.g90.backend.modules.project.repository;

import com.g90.backend.modules.project.entity.ProjectMilestoneEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestoneEntity, String> {

    List<ProjectMilestoneEntity> findByProject_IdOrderByCompletionPercentAsc(String projectId);

    Optional<ProjectMilestoneEntity> findByIdAndProject_Id(String id, String projectId);
}
