package com.g90.backend.modules.contract.entity;

import com.g90.backend.modules.product.entity.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Entity
@Table(name = "contract_items")
public class ContractItemEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ContractEntity contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @Column(name = "quantity", precision = 18, scale = 2)
    private BigDecimal quantity;

    @Column(name = "reserved_quantity", precision = 18, scale = 2)
    private BigDecimal reservedQuantity;

    @Column(name = "issued_quantity", precision = 18, scale = 2)
    private BigDecimal issuedQuantity;

    @Column(name = "delivered_quantity", precision = 18, scale = 2)
    private BigDecimal deliveredQuantity;

    @Column(name = "fulfillment_note", length = 500)
    private String fulfillmentNote;

    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "base_unit_price", precision = 18, scale = 2)
    private BigDecimal baseUnitPrice;

    @Column(name = "discount_amount", precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "price_override_reason", length = 500)
    private String priceOverrideReason;

    @Column(name = "total_price", precision = 18, scale = 2)
    private BigDecimal totalPrice;

    @PrePersist
    public void prePersist() {
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO.setScale(2);
        }
        if (reservedQuantity == null) {
            reservedQuantity = BigDecimal.ZERO.setScale(2);
        }
        if (issuedQuantity == null) {
            issuedQuantity = BigDecimal.ZERO.setScale(2);
        }
        if (deliveredQuantity == null) {
            deliveredQuantity = BigDecimal.ZERO.setScale(2);
        }
    }
}
