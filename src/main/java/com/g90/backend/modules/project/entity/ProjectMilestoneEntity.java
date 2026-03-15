package com.g90.backend.modules.project.entity;

import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Entity(name = "ProjectMilestoneManagementEntity")
@Table(name = "project_milestones")
public class ProjectMilestoneEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectManagementEntity project;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "milestone_type", length = 30, nullable = false)
    private String milestoneType;

    @Column(name = "completion_percent", nullable = false)
    private Integer completionPercent;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "confirmation_deadline")
    private LocalDateTime confirmationDeadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_customer_id")
    private CustomerProfileEntity confirmedByCustomer;

    @Column(name = "confirmation_status", length = 30, nullable = false)
    private String confirmationStatus;

    @Column(name = "confirmed")
    private Boolean confirmed;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "payment_release_ready", nullable = false)
    private Boolean paymentReleaseReady;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }
        if (!StringUtils.hasText(milestoneType)) {
            milestoneType = "PAYMENT";
        }
        if (!StringUtils.hasText(status)) {
            status = ProjectMilestoneStatus.PENDING.name();
        }
        if (!StringUtils.hasText(confirmationStatus)) {
            confirmationStatus = ProjectMilestoneStatus.PENDING.name();
        }
        if (confirmed == null) {
            confirmed = Boolean.FALSE;
        }
        if (paymentReleaseReady == null) {
            paymentReleaseReady = Boolean.FALSE;
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
