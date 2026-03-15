package com.g90.backend.modules.project.entity;

import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Entity(name = "ProjectManagementEntity")
@Table(name = "projects")
public class ProjectManagementEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "project_code", length = 50, unique = true)
    private String projectCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfileEntity customer;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "location", length = 500, nullable = false)
    private String location;

    @Column(name = "scope", length = 1000)
    private String scope;

    @Column(name = "budget", precision = 18, scale = 2, nullable = false)
    private BigDecimal budget;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private WarehouseEntity primaryWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backup_warehouse_id")
    private WarehouseEntity backupWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_contract_id")
    private ContractEntity linkedContract;

    @Column(name = "linked_order_reference", length = 100)
    private String linkedOrderReference;

    @Column(name = "assigned_project_manager", length = 255, nullable = false)
    private String assignedProjectManager;

    @Column(name = "progress_percent", nullable = false)
    private Integer progressPercent;

    @Column(name = "progress_status", length = 30, nullable = false)
    private String progressStatus;

    @Column(name = "current_phase", length = 50)
    private String currentPhase;

    @Column(name = "last_progress_update_at")
    private LocalDateTime lastProgressUpdateAt;

    @Column(name = "last_progress_note", length = 1000)
    private String lastProgressNote;

    @Column(name = "actual_spend", precision = 18, scale = 2, nullable = false)
    private BigDecimal actualSpend;

    @Column(name = "commitments", precision = 18, scale = 2, nullable = false)
    private BigDecimal commitments;

    @Column(name = "payments_received", precision = 18, scale = 2, nullable = false)
    private BigDecimal paymentsReceived;

    @Column(name = "payments_due", precision = 18, scale = 2, nullable = false)
    private BigDecimal paymentsDue;

    @Column(name = "outstanding_balance", precision = 18, scale = 2, nullable = false)
    private BigDecimal outstandingBalance;

    @Column(name = "open_order_count", nullable = false)
    private Integer openOrderCount;

    @Column(name = "unresolved_issue_count", nullable = false)
    private Integer unresolvedIssueCount;

    @Column(name = "budget_approval_status", length = 30, nullable = false)
    private String budgetApprovalStatus;

    @Column(name = "archive_approval_status", length = 30, nullable = false)
    private String archiveApprovalStatus;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "archived_by", length = 36)
    private String archivedBy;

    @Column(name = "archive_reason", length = 1000)
    private String archiveReason;

    @Column(name = "restore_deadline")
    private LocalDateTime restoreDeadline;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by", length = 36)
    private String closedBy;

    @Column(name = "close_reason", length = 1000)
    private String closeReason;

    @Column(name = "customer_signoff_completed", nullable = false)
    private Boolean customerSignoffCompleted;

    @Column(name = "customer_signoff_at")
    private LocalDateTime customerSignoffAt;

    @Column(name = "customer_satisfaction_score")
    private Integer customerSatisfactionScore;

    @Column(name = "warranty_start_date")
    private LocalDate warrantyStartDate;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMilestoneEntity> milestones = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }
        if (!StringUtils.hasText(status)) {
            status = ProjectStatus.ACTIVE.name();
        }
        if (!StringUtils.hasText(progressStatus)) {
            progressStatus = ProjectProgressStatus.ON_TRACK.name();
        }
        if (!StringUtils.hasText(budgetApprovalStatus)) {
            budgetApprovalStatus = ProjectApprovalStatus.NOT_REQUIRED.name();
        }
        if (!StringUtils.hasText(archiveApprovalStatus)) {
            archiveApprovalStatus = ProjectApprovalStatus.NOT_REQUIRED.name();
        }
        if (progressPercent == null) {
            progressPercent = 0;
        }
        if (actualSpend == null) {
            actualSpend = BigDecimal.ZERO.setScale(2);
        }
        if (commitments == null) {
            commitments = BigDecimal.ZERO.setScale(2);
        }
        if (paymentsReceived == null) {
            paymentsReceived = BigDecimal.ZERO.setScale(2);
        }
        if (paymentsDue == null) {
            paymentsDue = BigDecimal.ZERO.setScale(2);
        }
        if (outstandingBalance == null) {
            outstandingBalance = BigDecimal.ZERO.setScale(2);
        }
        if (openOrderCount == null) {
            openOrderCount = 0;
        }
        if (unresolvedIssueCount == null) {
            unresolvedIssueCount = 0;
        }
        if (customerSignoffCompleted == null) {
            customerSignoffCompleted = Boolean.FALSE;
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
