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
@Table(name = "contract_tracking_events")
public class ContractTrackingEventEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ContractEntity contract;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;

    @Column(name = "event_status", length = 30)
    private String eventStatus;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "expected_at")
    private LocalDateTime expectedAt;

    @Column(name = "actual_at")
    private LocalDateTime actualAt;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "created_by", length = 36, nullable = false)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now(APP_ZONE);
        }
    }
}
