package com.g90.backend.modules.inventory.mapper;

import com.g90.backend.modules.inventory.dto.InventoryHistoryItemResponse;
import com.g90.backend.modules.inventory.dto.InventoryMutationResponse;
import com.g90.backend.modules.inventory.dto.InventoryStatusItemResponse;
import com.g90.backend.modules.inventory.entity.InventoryTransactionEntity;
import com.g90.backend.modules.product.entity.ProductEntity;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public InventoryMutationResponse toMutationResponse(InventoryTransactionEntity transaction, String operatorEmail) {
        return InventoryMutationResponse.builder()
                .transactionId(transaction.getId())
                .transactionCode(transaction.getTransactionCode())
                .transactionType(transaction.getTransactionType())
                .productId(transaction.getProduct().getId())
                .productCode(transaction.getProduct().getProductCode())
                .productName(transaction.getProduct().getProductName())
                .quantity(transaction.getQuantity())
                .quantityBefore(transaction.getQuantityBefore())
                .quantityAfter(transaction.getQuantityAfter())
                .transactionDate(transaction.getTransactionDate() == null ? null : transaction.getTransactionDate().atZone(APP_ZONE).toOffsetDateTime())
                .operatorId(transaction.getCreatedBy())
                .operatorEmail(operatorEmail)
                .supplierName(transaction.getSupplierName())
                .relatedOrderId(transaction.getRelatedOrderId())
                .relatedProjectId(transaction.getRelatedProjectId())
                .reason(transaction.getReason())
                .note(transaction.getNote())
                .build();
    }

    public InventoryStatusItemResponse toStatusItem(ProductEntity product, BigDecimal quantity, java.time.LocalDateTime updatedAt) {
        return InventoryStatusItemResponse.builder()
                .productId(product.getId())
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .type(product.getType())
                .unit(product.getUnit())
                .currentQuantity(quantity)
                .updatedAt(updatedAt == null ? null : updatedAt.atZone(APP_ZONE).toOffsetDateTime())
                .build();
    }

    public InventoryHistoryItemResponse toHistoryItem(
            InventoryTransactionEntity transaction,
            Map<String, String> operatorEmails
    ) {
        return InventoryHistoryItemResponse.builder()
                .transactionId(transaction.getId())
                .transactionCode(transaction.getTransactionCode())
                .transactionType(transaction.getTransactionType())
                .productId(transaction.getProduct().getId())
                .productCode(transaction.getProduct().getProductCode())
                .productName(transaction.getProduct().getProductName())
                .quantity(transaction.getQuantity())
                .quantityBefore(transaction.getQuantityBefore())
                .quantityAfter(transaction.getQuantityAfter())
                .transactionDate(transaction.getTransactionDate() == null ? null : transaction.getTransactionDate().atZone(APP_ZONE).toOffsetDateTime())
                .operatorId(transaction.getCreatedBy())
                .operatorEmail(operatorEmails.get(transaction.getCreatedBy()))
                .supplierName(transaction.getSupplierName())
                .relatedOrderId(transaction.getRelatedOrderId())
                .relatedProjectId(transaction.getRelatedProjectId())
                .reason(transaction.getReason())
                .note(transaction.getNote())
                .build();
    }
}
