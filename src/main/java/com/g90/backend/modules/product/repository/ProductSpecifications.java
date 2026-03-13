package com.g90.backend.modules.product.repository;

import com.g90.backend.modules.product.dto.ProductListQuery;
import com.g90.backend.modules.product.entity.ProductEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<ProductEntity> withFilters(ProductListQuery query) {
        return Specification.allOf(
                keywordContains(query.getKeyword()),
                equalsIgnoreCase("type", query.getType()),
                equalsIgnoreCase("size", query.getSize()),
                equalsIgnoreCase("thickness", query.getThickness()),
                equalsIgnoreCase("unit", query.getUnit()),
                equalsIgnoreCase("status", query.getStatus())
        );
    }

    private static Specification<ProductEntity> keywordContains(String keyword) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productCode")), normalizedKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), normalizedKeyword)
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
