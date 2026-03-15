package com.g90.backend.modules.contract.entity;

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
@Entity
@Table(name = "contract_approvals")
public class ContractApprovalEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ContractEntity contract;

    @Column(name = "approval_type", length = 30, nullable = false)
    private String approvalType;

    @Column(name = "approval_tier", length = 30)
    private String approvalTier;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "requested_by", length = 36, nullable = false)
    private String requestedBy;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "decided_by", length = 36)
    private String decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "comment", length = 1000)
    private String comment;

    @PrePersist
    public void prePersist() {
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now(APP_ZONE);
        }
    }
}
