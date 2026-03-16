package com.g90.backend.modules.customer.repository;

import com.g90.backend.modules.customer.dto.CustomerListQuery;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    public static Specification<CustomerProfileEntity> withFilters(CustomerListQuery query) {
        return Specification.where(keywordContains(query.getKeyword()))
                .and(customerCodeContains(query.getCustomerCode()))
                .and(taxCodeContains(query.getTaxCode()))
                .and(typeEquals(query.getCustomerType()))
                .and(priceGroupEquals(query.getPriceGroup()))
                .and(statusEquals(query.getStatus()))
                .and(createdAtFrom(query.getCreatedFrom()))
                .and(createdAtTo(query.getCreatedTo()));
    }

    private static Specification<CustomerProfileEntity> keywordContains(String keyword) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }
            String normalized = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("companyName")), normalized),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("contactPerson")), normalized),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), normalized),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), normalized)
            );
        };
    }

    private static Specification<CustomerProfileEntity> customerCodeContains(String customerCode) {
        return likeIgnoreCase("customerCode", customerCode);
    }

    private static Specification<CustomerProfileEntity> taxCodeContains(String taxCode) {
        return likeIgnoreCase("taxCode", taxCode);
    }

    private static Specification<CustomerProfileEntity> typeEquals(String customerType) {
        return equalsIgnoreCase("customerType", customerType);
    }

    private static Specification<CustomerProfileEntity> priceGroupEquals(String priceGroup) {
        return equalsIgnoreCase("priceGroup", priceGroup);
    }

    private static Specification<CustomerProfileEntity> statusEquals(String status) {
        return equalsIgnoreCase("status", status);
    }

    private static Specification<CustomerProfileEntity> createdAtFrom(LocalDate createdFrom) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (createdFrom == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom.atStartOfDay());
        };
    }

    private static Specification<CustomerProfileEntity> createdAtTo(LocalDate createdTo) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (createdTo == null) {
                return criteriaBuilder.conjunction();
            }
            LocalDateTime end = createdTo.plusDays(1).atStartOfDay().minusNanos(1);
            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), end);
        };
    }

    private static Specification<CustomerProfileEntity> likeIgnoreCase(String field, String value) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(value)) {
                return criteriaBuilder.conjunction();
            }
            String normalized = "%" + value.trim().toLowerCase() + "%";
            return criteriaBuilder.like(criteriaBuilder.lower(root.get(field)), normalized);
        };
    }

    private static Specification<CustomerProfileEntity> equalsIgnoreCase(String field, String value) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(value)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(criteriaBuilder.upper(root.get(field)), value.trim().toUpperCase());
        };
    }
}
