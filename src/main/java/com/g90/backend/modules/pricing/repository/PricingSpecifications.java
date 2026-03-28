package com.g90.backend.modules.pricing.repository;

import com.g90.backend.modules.pricing.dto.PriceListListQuery;
import com.g90.backend.modules.pricing.entity.PriceListEntity;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PricingSpecifications {

    private PricingSpecifications() {
    }

    public static Specification<PriceListEntity> withFilters(PriceListListQuery query) {
        return Specification.where(notDeleted())
                .and(searchContains(query.getSearch()))
                .and(statusEquals(query.getStatus()))
                .and(customerGroupEquals(query.getCustomerGroup()))
                .and(validFrom(query.getValidFrom()))
                .and(validTo(query.getValidTo()));
    }

    private static Specification<PriceListEntity> notDeleted() {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedAt"));
    }

    private static Specification<PriceListEntity> searchContains(String search) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(search)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + search.trim().toLowerCase() + "%"
            );
        };
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

    private static Specification<PriceListEntity> validFrom(LocalDate validFrom) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (validFrom == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("validTo")),
                    criteriaBuilder.greaterThanOrEqualTo(root.get("validTo"), validFrom)
            );
        };
    }

    private static Specification<PriceListEntity> validTo(LocalDate validTo) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (validTo == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("validFrom")),
                    criteriaBuilder.lessThanOrEqualTo(root.get("validFrom"), validTo)
            );
        };
    }
}
