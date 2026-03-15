package com.g90.backend.modules.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Entity(name = "ProjectProgressUpdateEntity")
@Table(name = "project_progress_updates")
public class ProjectProgressUpdateEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectManagementEntity project;

    @Column(name = "previous_progress_percent", nullable = false)
    private Integer previousProgressPercent;

    @Column(name = "progress_percent", nullable = false)
    private Integer progressPercent;

    @Column(name = "progress_status", length = 30, nullable = false)
    private String progressStatus;

    @Column(name = "phase", length = 50)
    private String phase;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "change_reason", length = 1000)
    private String changeReason;

    @Column(name = "behind_schedule", nullable = false)
    private Boolean behindSchedule;

    @Column(name = "evidence_count", nullable = false)
    private Integer evidenceCount;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }
        if (behindSchedule == null) {
            behindSchedule = Boolean.FALSE;
        }
        if (evidenceCount == null) {
            evidenceCount = 0;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now(APP_ZONE);
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now(APP_ZONE);
    }
}
