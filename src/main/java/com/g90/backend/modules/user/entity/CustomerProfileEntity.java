package com.g90.backend.modules.user.entity;

import com.g90.backend.modules.account.entity.UserAccountEntity;
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
@Entity
@Table(name = "customers")
public class CustomerProfileEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "customer_code", length = 50, unique = true)
    private String customerCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccountEntity user;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "tax_code", length = 20)
    private String taxCode;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "contact_person", length = 255)
    private String contactPerson;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "customer_type", length = 50)
    private String customerType;

    @Column(name = "price_group", length = 50)
    private String priceGroup;

    @Column(name = "credit_limit", precision = 18, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "payment_terms", length = 255)
    private String paymentTerms;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }
        if (creditLimit == null) {
            creditLimit = BigDecimal.ZERO.setScale(2);
        }
        if (!StringUtils.hasText(status)) {
            status = "ACTIVE";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now(APP_ZONE);
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (!StringUtils.hasText(priceGroup) && StringUtils.hasText(customerType)) {
            priceGroup = customerType.trim();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now(APP_ZONE);
        if (!StringUtils.hasText(priceGroup) && StringUtils.hasText(customerType)) {
            priceGroup = customerType.trim();
        }
    }
}
