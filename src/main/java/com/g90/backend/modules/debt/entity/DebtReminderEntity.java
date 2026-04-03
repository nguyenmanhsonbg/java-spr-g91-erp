package com.g90.backend.modules.debt.entity;

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
@Entity(name = "DebtReminderEntity")
@Table(name = "debt_reminders")
public class DebtReminderEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private DebtInvoiceEntity invoice;

    @Column(name = "reminder_type", length = 20, nullable = false)
    private String reminderType;

    @Column(name = "channel", length = 20, nullable = false)
    private String channel;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "sent_by", length = 36, nullable = false)
    private String sentBy;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }
        if (sentAt == null) {
            sentAt = LocalDateTime.now(APP_ZONE);
        }
        if (createdAt == null) {
            createdAt = sentAt;
        }
    }
}
