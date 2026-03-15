package com.g90.backend.modules.contract.entity;

import com.g90.backend.modules.quotation.entity.QuotationEntity;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
@Entity
@Table(name = "contracts")
public class ContractEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "contract_number", length = 50)
    private String contractNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerProfileEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id")
    private QuotationEntity quotation;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "payment_terms", length = 255)
    private String paymentTerms;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(name = "delivery_terms", length = 1000)
    private String deliveryTerms;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "confidential")
    private boolean confidential;

    @Column(name = "requires_approval")
    private boolean requiresApproval;

    @Column(name = "approval_status", length = 30)
    private String approvalStatus;

    @Column(name = "approval_tier", length = 30)
    private String approvalTier;

    @Column(name = "pending_action", length = 30)
    private String pendingAction;

    @Column(name = "approval_requested_at")
    private LocalDateTime approvalRequestedAt;

    @Column(name = "approval_due_at")
    private LocalDateTime approvalDueAt;

    @Column(name = "approved_by", length = 36)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "credit_limit_snapshot", precision = 18, scale = 2)
    private BigDecimal creditLimitSnapshot;

    @Column(name = "current_debt_snapshot", precision = 18, scale = 2)
    private BigDecimal currentDebtSnapshot;

    @Column(name = "deposit_percentage", precision = 5, scale = 2)
    private BigDecimal depositPercentage;

    @Column(name = "deposit_amount", precision = 18, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @Column(name = "submitted_by", length = 36)
    private String submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "cancelled_by", length = 36)
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason_code", length = 50)
    private String cancellationReasonCode;

    @Column(name = "cancellation_note", length = 1000)
    private String cancellationNote;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "auto_submit_due_at")
    private LocalDateTime autoSubmitDueAt;

    @Column(name = "price_change_percent", precision = 10, scale = 2)
    private BigDecimal priceChangePercent;

    @Column(name = "last_status_change_at")
    private LocalDateTime lastStatusChangeAt;

    @Column(name = "last_tracking_refresh_at")
    private LocalDateTime lastTrackingRefreshAt;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<ContractItemEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNo desc")
    private List<ContractVersionEntity> versions = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("requestedAt desc")
    private List<ContractApprovalEntity> approvals = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt asc")
    private List<ContractStatusHistoryEntity> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("generatedAt desc")
    private List<ContractDocumentEntity> documents = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt asc")
    private List<ContractTrackingEventEntity> trackingEvents = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now(APP_ZONE);
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (!StringUtils.hasText(approvalStatus)) {
            approvalStatus = ContractApprovalStatus.NOT_REQUIRED.name();
        }
        if (lastStatusChangeAt == null) {
            lastStatusChangeAt = createdAt;
        }
        if (lastTrackingRefreshAt == null) {
            lastTrackingRefreshAt = createdAt;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now(APP_ZONE);
    }
}
