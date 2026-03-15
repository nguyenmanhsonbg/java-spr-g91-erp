package com.g90.backend.modules.project.repository;

import com.g90.backend.modules.project.entity.ProjectDocumentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectDocumentRepository extends JpaRepository<ProjectDocumentEntity, String> {

    List<ProjectDocumentEntity> findByProject_IdOrderByUploadedAtDesc(String projectId);

    List<ProjectDocumentEntity> findByProgressUpdate_Id(String progressUpdateId);
}
