package com.g90.backend.modules.inventory.repository;

import com.g90.backend.modules.inventory.dto.InventoryStatusQuery;
import com.g90.backend.modules.product.entity.ProductEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class InventoryProductSpecifications {

    private InventoryProductSpecifications() {
    }

    public static Specification<ProductEntity> withFilters(InventoryStatusQuery query) {
        return Specification.allOf(
                activeCatalogScope(),
                productIdEquals(query.getProductId()),
                keywordContains(query.getSearch())
        );
    }

    private static Specification<ProductEntity> activeCatalogScope() {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedAt"));
    }

    private static Specification<ProductEntity> productIdEquals(String productId) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(productId)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("id"), productId.trim());
        };
    }

    private static Specification<ProductEntity> keywordContains(String search) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(search)) {
                return criteriaBuilder.conjunction();
            }

            String keyword = "%" + search.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productCode")), keyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), keyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("type")), keyword)
            );
        };
    }
}
