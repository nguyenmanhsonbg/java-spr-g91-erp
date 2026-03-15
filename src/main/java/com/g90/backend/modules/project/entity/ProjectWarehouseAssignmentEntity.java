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
@Entity(name = "ProjectWarehouseAssignmentEntity")
@Table(name = "project_warehouse_assignments")
public class ProjectWarehouseAssignmentEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectManagementEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private WarehouseEntity warehouse;

    @Column(name = "assignment_type", length = 20, nullable = false)
    private String assignmentType;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "assignment_reason", length = 1000)
    private String assignmentReason;

    @Column(name = "assigned_by", length = 36)
    private String assignedBy;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @PrePersist
    public void prePersist() {
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }
        if (!StringUtils.hasText(assignmentType)) {
            assignmentType = ProjectWarehouseAssignmentType.PRIMARY.name();
        }
        if (active == null) {
            active = Boolean.TRUE;
        }
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now(APP_ZONE);
        }
    }
}
