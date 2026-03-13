package com.g90.backend.modules.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class ProductEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "product_code", length = 50, nullable = false, unique = true)
    private String productCode;

    @Column(name = "product_name", length = 255, nullable = false)
    private String productName;

    @Column(name = "type", length = 100, nullable = false)
    private String type;

    @Column(name = "size", length = 100, nullable = false)
    private String size;

    @Column(name = "thickness", length = 50, nullable = false)
    private String thickness;

    @Column(name = "unit", length = 20, nullable = false)
    private String unit;

    @Column(name = "weight_conversion", precision = 10, scale = 4)
    private BigDecimal weightConversion;

    @Column(name = "reference_weight", precision = 10, scale = 4)
    private BigDecimal referenceWeight;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }
        if (!StringUtils.hasText(status)) {
            status = ProductStatus.ACTIVE.name();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now(APP_ZONE);
        }
    }
}
