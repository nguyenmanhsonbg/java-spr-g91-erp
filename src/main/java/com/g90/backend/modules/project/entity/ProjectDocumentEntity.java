package com.g90.backend.modules.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Entity(name = "ProjectDocumentEntity")
@Table(name = "project_documents")
public class ProjectDocumentEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectManagementEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "progress_update_id")
    private ProjectProgressUpdateEntity progressUpdate;

    @Column(name = "document_type", length = 30, nullable = false)
    private String documentType;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "file_url", length = 500, nullable = false)
    private String fileUrl;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "uploaded_by", length = 36)
    private String uploadedBy;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() {
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }
        if (!StringUtils.hasText(documentType)) {
            documentType = ProjectDocumentType.DOCUMENT.name();
        }
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now(APP_ZONE);
        }
    }
}
