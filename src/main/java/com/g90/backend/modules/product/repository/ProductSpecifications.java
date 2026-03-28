package com.g90.backend.modules.product.repository;

import com.g90.backend.modules.product.dto.ProductListQuery;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.entity.ProductStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<ProductEntity> withFilters(ProductListQuery query) {
        return withFilters(query, false);
    }

    public static Specification<ProductEntity> withFilters(ProductListQuery query, boolean warehouseView) {
        return Specification.allOf(
                visibilityScope(warehouseView),
                keywordContains(query.getKeyword()),
                equalsIgnoreCase("type", query.getType()),
                equalsIgnoreCase("size", query.getSize()),
                equalsIgnoreCase("thickness", query.getThickness()),
                equalsIgnoreCase("unit", query.getUnit()),
                equalsIgnoreCase("status", query.getStatus())
        );
    }

    private static Specification<ProductEntity> visibilityScope(boolean warehouseView) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (warehouseView) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.and(
                    criteriaBuilder.equal(criteriaBuilder.upper(root.get("status")), ProductStatus.ACTIVE.name()),
                    criteriaBuilder.isNull(root.get("deletedAt"))
            );
        };
    }

    private static Specification<ProductEntity> keywordContains(String keyword) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productCode")), normalizedKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), normalizedKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("type")), normalizedKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("size")), normalizedKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("thickness")), normalizedKeyword)
            );
        };
    }

    private static Specification<ProductEntity> equalsIgnoreCase(String field, String value) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(value)) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(criteriaBuilder.lower(root.get(field)), value.trim().toLowerCase());
        };
    }
}
