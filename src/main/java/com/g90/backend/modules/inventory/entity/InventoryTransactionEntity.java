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
@Table(name = "inventory_transactions")
public class InventoryTransactionEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "transaction_code", length = 50)
    private String transactionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private WarehouseEntity warehouse;

    @Column(name = "transaction_type", length = 50, nullable = false)
    private String transactionType;

    @Column(name = "quantity", precision = 18, scale = 2, nullable = false)
    private BigDecimal quantity;

    @Column(name = "quantity_before", precision = 18, scale = 2, nullable = false)
    private BigDecimal quantityBefore;

    @Column(name = "quantity_after", precision = 18, scale = 2, nullable = false)
    private BigDecimal quantityAfter;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id", length = 36)
    private String referenceId;

    @Column(name = "supplier_name", length = 255)
    private String supplierName;

    @Column(name = "related_order_id", length = 36)
    private String relatedOrderId;

    @Column(name = "related_project_id", length = 36)
    private String relatedProjectId;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_by", length = 36)
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
        if (transactionDate == null) {
            transactionDate = createdAt;
        }
    }
}
