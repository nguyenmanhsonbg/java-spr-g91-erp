package com.g90.backend.modules.project.repository;

import com.g90.backend.modules.project.entity.ProjectProgressUpdateEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectProgressUpdateRepository extends JpaRepository<ProjectProgressUpdateEntity, String> {

    List<ProjectProgressUpdateEntity> findByProject_IdOrderByCreatedAtDesc(String projectId);

    Optional<ProjectProgressUpdateEntity> findByIdAndProject_Id(String id, String projectId);
}
