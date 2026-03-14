package com.g90.backend.modules.pricing.repository;

import com.g90.backend.modules.pricing.dto.PriceListListQuery;
import com.g90.backend.modules.pricing.entity.PriceListEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PricingSpecifications {

    private PricingSpecifications() {
    }

    public static Specification<PriceListEntity> withFilters(PriceListListQuery query) {
        return Specification.where(statusEquals(query.getStatus()))
                .and(customerGroupEquals(query.getCustomerGroup()));
    }

    private static Specification<PriceListEntity> statusEquals(String status) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(status)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                    criteriaBuilder.upper(root.get("status")),
                    status.trim().toUpperCase()
            );
        };
    }

    private static Specification<PriceListEntity> customerGroupEquals(String customerGroup) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(customerGroup)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                    criteriaBuilder.upper(root.get("customerGroup")),
                    customerGroup.trim().toUpperCase()
            );
        };
    }
}
