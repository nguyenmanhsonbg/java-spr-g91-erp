package com.g90.backend.modules.debt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Entity(name = "DebtSettlementEntity")
@Table(name = "debt_settlements")
public class DebtSettlementEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "confirmed_by", length = 36, nullable = false)
    private String confirmedBy;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "certificate_url", length = 500)
    private String certificateUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }
        if (settlementDate == null) {
            settlementDate = LocalDate.now(APP_ZONE);
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now(APP_ZONE);
        }
    }
}
