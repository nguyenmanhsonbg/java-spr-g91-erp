package com.g90.backend.modules.inventory.repository;

import com.g90.backend.modules.inventory.dto.InventoryHistoryQuery;
import com.g90.backend.modules.inventory.entity.InventoryTransactionEntity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class InventoryTransactionSpecifications {

    private InventoryTransactionSpecifications() {
    }

    public static Specification<InventoryTransactionEntity> withFilters(InventoryHistoryQuery query) {
        return Specification.allOf(
                productEquals(query.getProductId()),
                relatedOrderEquals(query.getRelatedOrderId()),
                typeEquals(query.getTransactionType()),
                fromDate(query.getFromDate() == null ? null : query.getFromDate().atStartOfDay()),
                toDate(query.getToDate() == null ? null : query.getToDate().atTime(23, 59, 59))
        );
    }

    private static Specification<InventoryTransactionEntity> productEquals(String productId) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(productId)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("product").get("id"), productId.trim());
        };
    }

    private static Specification<InventoryTransactionEntity> typeEquals(String transactionType) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(transactionType)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(criteriaBuilder.upper(root.get("transactionType")), transactionType.trim().toUpperCase());
        };
    }

    private static Specification<InventoryTransactionEntity> relatedOrderEquals(String relatedOrderId) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(relatedOrderId)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("relatedOrderId"), relatedOrderId.trim());
        };
    }

    private static Specification<InventoryTransactionEntity> fromDate(LocalDateTime fromDate) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (fromDate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), fromDate);
        };
    }

    private static Specification<InventoryTransactionEntity> toDate(LocalDateTime toDate) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (toDate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), toDate);
        };
    }
}
