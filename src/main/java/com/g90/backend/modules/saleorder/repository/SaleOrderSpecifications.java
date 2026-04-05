package com.g90.backend.modules.saleorder.repository;

import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.project.entity.ProjectManagementEntity;
import com.g90.backend.modules.saleorder.dto.SaleOrderListQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class SaleOrderSpecifications {

    private SaleOrderSpecifications() {
    }

    public static Specification<ContractEntity> byQuery(SaleOrderListQuery query, String customerScope) {
        return Specification.where(executableOrder())
                .and(customerScope(customerScope))
                .and(saleOrderNumberContains(query.getSaleOrderNumber()))
                .and(contractNumberContains(query.getContractNumber()))
                .and(customerEquals(query.getCustomerId()))
                .and(projectEquals(query.getProjectId()))
                .and(statusEquals(query.getStatus()))
                .and(orderFrom(query.getOrderFrom()))
                .and(orderTo(query.getOrderTo()))
                .and(expectedDeliveryFrom(query.getDeliveryFrom()))
                .and(expectedDeliveryTo(query.getDeliveryTo()))
                .and(keywordContains(query.getKeyword()));
    }

    private static Specification<ContractEntity> executableOrder() {
        return (root, query, builder) -> builder.or(
                builder.isNotNull(root.get("submittedAt")),
                builder.isNotNull(root.get("saleOrderNumber"))
        );
    }

    private static Specification<ContractEntity> customerScope(String customerId) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(customerId)) {
                return builder.conjunction();
            }
            return builder.equal(root.get("customer").get("id"), customerId.trim());
        };
    }

    private static Specification<ContractEntity> saleOrderNumberContains(String saleOrderNumber) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(saleOrderNumber)) {
                return builder.conjunction();
            }
            return builder.like(builder.lower(root.get("saleOrderNumber")), "%" + saleOrderNumber.trim().toLowerCase() + "%");
        };
    }

    private static Specification<ContractEntity> contractNumberContains(String contractNumber) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(contractNumber)) {
                return builder.conjunction();
            }
            return builder.like(builder.lower(root.get("contractNumber")), "%" + contractNumber.trim().toLowerCase() + "%");
        };
    }

    private static Specification<ContractEntity> customerEquals(String customerId) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(customerId)) {
                return builder.conjunction();
            }
            return builder.equal(root.get("customer").get("id"), customerId.trim());
        };
    }

    private static Specification<ContractEntity> projectEquals(String projectId) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(projectId)) {
                return builder.conjunction();
            }
            var subquery = query.subquery(String.class);
            var projectRoot = subquery.from(ProjectManagementEntity.class);
            subquery.select(projectRoot.get("id"));
            subquery.where(
                    builder.equal(projectRoot.get("id"), projectId.trim()),
                    builder.equal(projectRoot.get("linkedContract").get("id"), root.get("id"))
            );
            return builder.exists(subquery);
        };
    }

    private static Specification<ContractEntity> statusEquals(String status) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(status)) {
                return builder.conjunction();
            }
            return builder.equal(builder.upper(root.get("status")), status.trim().toUpperCase());
        };
    }

    private static Specification<ContractEntity> orderFrom(LocalDate orderFrom) {
        return (root, query, builder) -> {
            if (orderFrom == null) {
                return builder.conjunction();
            }
            return builder.greaterThanOrEqualTo(root.get("submittedAt"), orderFrom.atStartOfDay());
        };
    }

    private static Specification<ContractEntity> orderTo(LocalDate orderTo) {
        return (root, query, builder) -> {
            if (orderTo == null) {
                return builder.conjunction();
            }
            LocalDateTime endOfDay = orderTo.plusDays(1).atStartOfDay().minusNanos(1);
            return builder.lessThanOrEqualTo(root.get("submittedAt"), endOfDay);
        };
    }

    private static Specification<ContractEntity> expectedDeliveryFrom(LocalDate deliveryFrom) {
        return (root, query, builder) -> deliveryFrom == null
                ? builder.conjunction()
                : builder.greaterThanOrEqualTo(root.get("expectedDeliveryDate"), deliveryFrom);
    }

    private static Specification<ContractEntity> expectedDeliveryTo(LocalDate deliveryTo) {
        return (root, query, builder) -> deliveryTo == null
                ? builder.conjunction()
                : builder.lessThanOrEqualTo(root.get("expectedDeliveryDate"), deliveryTo);
    }

    private static Specification<ContractEntity> keywordContains(String keyword) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(keyword)) {
                return builder.conjunction();
            }
            String normalized = "%" + keyword.trim().toLowerCase() + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("saleOrderNumber")), normalized),
                    builder.like(builder.lower(root.get("contractNumber")), normalized),
                    builder.like(builder.lower(root.get("customer").get("companyName")), normalized)
            );
        };
    }
}
