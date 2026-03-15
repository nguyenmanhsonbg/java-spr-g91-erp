package com.g90.backend.modules.contract.repository;

import com.g90.backend.modules.contract.dto.ContractListQuery;
import com.g90.backend.modules.contract.dto.PendingContractApprovalListQuery;
import com.g90.backend.modules.contract.entity.ContractEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ContractSpecifications {

    private ContractSpecifications() {
    }

    public static Specification<ContractEntity> byQuery(ContractListQuery query, String customerId) {
        return Specification.where(customerScope(customerId))
                .and(numberContains(query.getContractNumber()))
                .and(customerEquals(query.getCustomerId()))
                .and(statusEquals(query.getStatus()))
                .and(approvalStatusEquals(query.getApprovalStatus()))
                .and(createdAtFrom(query.getCreatedFrom()))
                .and(createdAtTo(query.getCreatedTo()))
                .and(expectedDeliveryFrom(query.getDeliveryFrom()))
                .and(expectedDeliveryTo(query.getDeliveryTo()))
                .and(confidentialEquals(query.getConfidential()))
                .and(submittedFilter(query.getSubmitted()))
                .and(keywordContains(query.getKeyword()));
    }

    public static Specification<ContractEntity> byPendingApprovalQuery(PendingContractApprovalListQuery query) {
        return Specification.where(approvalStatusEquals("PENDING"))
                .and(customerEquals(query.getCustomerId()))
                .and(pendingActionEquals(query.getPendingAction()))
                .and(approvalTierEquals(query.getApprovalTier()))
                .and(approvalRequestedFrom(query.getRequestedFrom()))
                .and(approvalRequestedTo(query.getRequestedTo()))
                .and(totalAmountFrom(query.getMinAmount()))
                .and(totalAmountTo(query.getMaxAmount()))
                .and(keywordContains(query.getKeyword()));
    }

    private static Specification<ContractEntity> customerScope(String customerId) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(customerId)) {
                return builder.conjunction();
            }
            return builder.equal(root.get("customer").get("id"), customerId);
        };
    }

    private static Specification<ContractEntity> numberContains(String contractNumber) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(contractNumber)) {
                return builder.conjunction();
            }
            String normalized = "%" + contractNumber.trim().toLowerCase() + "%";
            return builder.like(builder.lower(root.get("contractNumber")), normalized);
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

    private static Specification<ContractEntity> statusEquals(String status) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(status)) {
                return builder.conjunction();
            }
            return builder.equal(builder.upper(root.get("status")), status.trim().toUpperCase());
        };
    }

    private static Specification<ContractEntity> approvalStatusEquals(String approvalStatus) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(approvalStatus)) {
                return builder.conjunction();
            }
            return builder.equal(builder.upper(root.get("approvalStatus")), approvalStatus.trim().toUpperCase());
        };
    }

    private static Specification<ContractEntity> pendingActionEquals(String pendingAction) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(pendingAction)) {
                return builder.conjunction();
            }
            return builder.equal(builder.upper(root.get("pendingAction")), pendingAction.trim().toUpperCase());
        };
    }

    private static Specification<ContractEntity> approvalTierEquals(String approvalTier) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(approvalTier)) {
                return builder.conjunction();
            }
            return builder.equal(builder.upper(root.get("approvalTier")), approvalTier.trim().toUpperCase());
        };
    }

    private static Specification<ContractEntity> createdAtFrom(LocalDate createdFrom) {
        return (root, query, builder) -> {
            if (createdFrom == null) {
                return builder.conjunction();
            }
            return builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom.atStartOfDay());
        };
    }

    private static Specification<ContractEntity> createdAtTo(LocalDate createdTo) {
        return (root, query, builder) -> {
            if (createdTo == null) {
                return builder.conjunction();
            }
            LocalDateTime endOfDay = createdTo.plusDays(1).atStartOfDay().minusNanos(1);
            return builder.lessThanOrEqualTo(root.get("createdAt"), endOfDay);
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

    private static Specification<ContractEntity> approvalRequestedFrom(LocalDate requestedFrom) {
        return (root, query, builder) -> {
            if (requestedFrom == null) {
                return builder.conjunction();
            }
            return builder.greaterThanOrEqualTo(root.get("approvalRequestedAt"), requestedFrom.atStartOfDay());
        };
    }

    private static Specification<ContractEntity> approvalRequestedTo(LocalDate requestedTo) {
        return (root, query, builder) -> {
            if (requestedTo == null) {
                return builder.conjunction();
            }
            LocalDateTime endOfDay = requestedTo.plusDays(1).atStartOfDay().minusNanos(1);
            return builder.lessThanOrEqualTo(root.get("approvalRequestedAt"), endOfDay);
        };
    }

    private static Specification<ContractEntity> totalAmountFrom(BigDecimal minAmount) {
        return (root, query, builder) -> minAmount == null
                ? builder.conjunction()
                : builder.greaterThanOrEqualTo(root.get("totalAmount"), minAmount);
    }

    private static Specification<ContractEntity> totalAmountTo(BigDecimal maxAmount) {
        return (root, query, builder) -> maxAmount == null
                ? builder.conjunction()
                : builder.lessThanOrEqualTo(root.get("totalAmount"), maxAmount);
    }

    private static Specification<ContractEntity> confidentialEquals(Boolean confidential) {
        return (root, query, builder) -> confidential == null
                ? builder.conjunction()
                : builder.equal(root.get("confidential"), confidential);
    }

    private static Specification<ContractEntity> submittedFilter(Boolean submitted) {
        return (root, query, builder) -> {
            if (submitted == null) {
                return builder.conjunction();
            }
            return submitted
                    ? builder.isNotNull(root.get("submittedAt"))
                    : builder.isNull(root.get("submittedAt"));
        };
    }

    private static Specification<ContractEntity> keywordContains(String keyword) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(keyword)) {
                return builder.conjunction();
            }
            String normalized = "%" + keyword.trim().toLowerCase() + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("contractNumber")), normalized),
                    builder.like(builder.lower(root.get("customer").get("companyName")), normalized),
                    builder.like(builder.lower(root.get("deliveryAddress")), normalized)
            );
        };
    }
}
