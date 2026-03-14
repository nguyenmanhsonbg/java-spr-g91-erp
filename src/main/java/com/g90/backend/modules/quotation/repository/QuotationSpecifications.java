package com.g90.backend.modules.quotation.repository;

import com.g90.backend.modules.quotation.dto.CustomerQuotationListQuery;
import com.g90.backend.modules.quotation.entity.QuotationEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class QuotationSpecifications {

    private QuotationSpecifications() {
    }

    public static Specification<QuotationEntity> forCustomer(String customerId, CustomerQuotationListQuery query) {
        return Specification.where(customerEquals(customerId))
                .and(keywordContains(query.getKeyword()))
                .and(statusEquals(query.getStatus()))
                .and(createdAtFrom(query.getFromDate()))
                .and(createdAtTo(query.getToDate()));
    }

    private static Specification<QuotationEntity> customerEquals(String customerId) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("customer").get("id"), customerId);
    }

    private static Specification<QuotationEntity> keywordContains(String keyword) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }
            String normalized = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("quotationNumber")), normalized);
        };
    }

    private static Specification<QuotationEntity> statusEquals(String status) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(status)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(criteriaBuilder.upper(root.get("status")), status.trim().toUpperCase());
        };
    }

    private static Specification<QuotationEntity> createdAtFrom(LocalDate fromDate) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (fromDate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay());
        };
    }

    private static Specification<QuotationEntity> createdAtTo(LocalDate toDate) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (toDate == null) {
                return criteriaBuilder.conjunction();
            }
            LocalDateTime end = toDate.plusDays(1).atStartOfDay().minusNanos(1);
            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), end);
        };
    }
}
