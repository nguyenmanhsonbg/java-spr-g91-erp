package com.g90.backend.modules.inventory.entity;

import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.project.entity.WarehouseEntity;
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
@Table(name = "inventory")
public class InventoryStockEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private WarehouseEntity warehouse;

    @Column(name = "quantity", precision = 18, scale = 2, nullable = false)
    private BigDecimal quantity;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }
        if (quantity == null) {
            quantity = BigDecimal.ZERO;
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now(APP_ZONE);
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now(APP_ZONE);
    }
}
