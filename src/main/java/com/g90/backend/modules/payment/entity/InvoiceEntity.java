package com.g90.backend.modules.payment.entity;

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
@Entity(name = "PaymentInvoiceEntity")
@Table(name = "invoices")
public class InvoiceEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ContractEntity contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerProfileEntity customer;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "customer_tax_code", length = 20)
    private String customerTaxCode;

    @Column(name = "billing_address", length = 500)
    private String billingAddress;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "payment_terms", length = 255)
    private String paymentTerms;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "adjustment_amount", precision = 18, scale = 2)
    private BigDecimal adjustmentAmount;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "vat_amount", precision = 18, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "document_url", length = 1000)
    private String documentUrl;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @Column(name = "issued_by", length = 36)
    private String issuedBy;

    @Column(name = "cancelled_by", length = 36)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<InvoiceItemEntity> items = new ArrayList<>();

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
