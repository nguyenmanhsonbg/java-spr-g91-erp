package com.g90.backend.modules.payment.entity;

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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Entity(name = "PaymentConfirmationRequestEntity")
@Table(name = "payment_confirmation_requests")
public class PaymentConfirmationRequestEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfileEntity customer;

    @Column(name = "requested_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal requestedAmount;

    @Column(name = "confirmed_amount", precision = 18, scale = 2)
    private BigDecimal confirmedAmount;

    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Column(name = "transfer_time", nullable = false)
    private LocalDateTime transferTime;

    @Column(name = "sender_bank_name", length = 255, nullable = false)
    private String senderBankName;

    @Column(name = "sender_account_name", length = 255, nullable = false)
    private String senderAccountName;

    @Column(name = "sender_account_no", length = 100, nullable = false)
    private String senderAccountNo;

    @Column(name = "reference_code", length = 100, nullable = false)
    private String referenceCode;

    @Column(name = "proof_document_url", length = 1000)
    private String proofDocumentUrl;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now(APP_ZONE);
    }
}
