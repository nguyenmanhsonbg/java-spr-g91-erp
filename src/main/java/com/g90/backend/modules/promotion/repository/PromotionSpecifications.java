package com.g90.backend.modules.promotion.repository;

import com.g90.backend.modules.promotion.dto.PromotionListQuery;
import com.g90.backend.modules.promotion.entity.PromotionCustomerGroupEntity;
import com.g90.backend.modules.promotion.entity.PromotionEntity;
import com.g90.backend.modules.promotion.entity.PromotionProductEntity;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PromotionSpecifications {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private PromotionSpecifications() {
    }

    public static Specification<PromotionEntity> withFilters(
            PromotionListQuery query,
            boolean internalView,
            Set<String> viewerCustomerGroups
    ) {
        return Specification.where(distinct())
                .and(notDeleted())
                .and(searchContains(query.getSearch()))
                .and(statusEquals(query.getStatus()))
                .and(typeEquals(query.getPromotionType()))
                .and(validFrom(query.getValidFrom()))
                .and(validTo(query.getValidTo()))
                .and(productIdMatches(query.getProductId()))
                .and(customerGroupMatches(query.getCustomerGroup()))
                .and(customerVisibility(internalView, viewerCustomerGroups));
    }

    private static Specification<PromotionEntity> distinct() {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            return criteriaBuilder.conjunction();
        };
    }

    private static Specification<PromotionEntity> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedAt"));
    }

    private static Specification<PromotionEntity> searchContains(String search) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(search)) {
                return criteriaBuilder.conjunction();
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern)
            );
        };
    }

    private static Specification<PromotionEntity> statusEquals(String status) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(status)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(criteriaBuilder.upper(root.get("status")), status.trim().toUpperCase());
        };
    }

    private static Specification<PromotionEntity> typeEquals(String promotionType) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(promotionType)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(criteriaBuilder.upper(root.get("promotionType")), promotionType.trim().toUpperCase());
        };
    }

    private static Specification<PromotionEntity> validFrom(LocalDate validFrom) {
        return (root, query, criteriaBuilder) -> {
            if (validFrom == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("endDate")),
                    criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), validFrom)
            );
        };
    }

    private static Specification<PromotionEntity> validTo(LocalDate validTo) {
        return (root, query, criteriaBuilder) -> {
            if (validTo == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("startDate")),
                    criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), validTo)
            );
        };
    }

    private static Specification<PromotionEntity> productIdMatches(String productId) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(productId)) {
                return criteriaBuilder.conjunction();
            }

            var noScope = query.subquery(String.class);
            var noScopeRoot = noScope.from(PromotionProductEntity.class);
            noScope.select(noScopeRoot.get("id"));
            noScope.where(
                    criteriaBuilder.equal(noScopeRoot.get("promotion"), root),
                    criteriaBuilder.isNull(noScopeRoot.get("deletedAt"))
            );

            var matchingScope = query.subquery(String.class);
            var matchingRoot = matchingScope.from(PromotionProductEntity.class);
            matchingScope.select(matchingRoot.get("id"));
            matchingScope.where(
                    criteriaBuilder.equal(matchingRoot.get("promotion"), root),
                    criteriaBuilder.isNull(matchingRoot.get("deletedAt")),
                    criteriaBuilder.equal(matchingRoot.get("product").get("id"), productId.trim())
            );

            return criteriaBuilder.or(
                    criteriaBuilder.not(criteriaBuilder.exists(noScope)),
                    criteriaBuilder.exists(matchingScope)
            );
        };
    }

    private static Specification<PromotionEntity> customerGroupMatches(String customerGroup) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(customerGroup)) {
                return criteriaBuilder.conjunction();
            }

            String normalized = customerGroup.trim().toUpperCase();
            var noScope = query.subquery(String.class);
            var noScopeRoot = noScope.from(PromotionCustomerGroupEntity.class);
            noScope.select(noScopeRoot.get("id"));
            noScope.where(
                    criteriaBuilder.equal(noScopeRoot.get("promotion"), root),
                    criteriaBuilder.isNull(noScopeRoot.get("deletedAt"))
            );

            var matchingScope = query.subquery(String.class);
            var matchingRoot = matchingScope.from(PromotionCustomerGroupEntity.class);
            matchingScope.select(matchingRoot.get("id"));
            matchingScope.where(
                    criteriaBuilder.equal(matchingRoot.get("promotion"), root),
                    criteriaBuilder.isNull(matchingRoot.get("deletedAt")),
                    criteriaBuilder.equal(criteriaBuilder.upper(matchingRoot.get("customerGroup")), normalized)
            );

            return criteriaBuilder.or(
                    criteriaBuilder.not(criteriaBuilder.exists(noScope)),
                    criteriaBuilder.exists(matchingScope)
            );
        };
    }

    private static Specification<PromotionEntity> customerVisibility(boolean internalView, Set<String> viewerCustomerGroups) {
        return (root, query, criteriaBuilder) -> {
            if (internalView) {
                return criteriaBuilder.conjunction();
            }

            LocalDate today = LocalDate.now(APP_ZONE);
            var noScope = query.subquery(String.class);
            var noScopeRoot = noScope.from(PromotionCustomerGroupEntity.class);
            noScope.select(noScopeRoot.get("id"));
            noScope.where(
                    criteriaBuilder.equal(noScopeRoot.get("promotion"), root),
                    criteriaBuilder.isNull(noScopeRoot.get("deletedAt"))
            );

            var matchingScope = query.subquery(String.class);
            var matchingRoot = matchingScope.from(PromotionCustomerGroupEntity.class);
            matchingScope.select(matchingRoot.get("id"));
            matchingScope.where(
                    criteriaBuilder.equal(matchingRoot.get("promotion"), root),
                    criteriaBuilder.isNull(matchingRoot.get("deletedAt")),
                    criteriaBuilder.upper(matchingRoot.get("customerGroup")).in(viewerCustomerGroups)
            );

            return criteriaBuilder.and(
                    criteriaBuilder.equal(criteriaBuilder.upper(root.get("status")), "ACTIVE"),
                    criteriaBuilder.or(criteriaBuilder.isNull(root.get("startDate")), criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), today)),
                    criteriaBuilder.or(criteriaBuilder.isNull(root.get("endDate")), criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), today)),
                    criteriaBuilder.or(
                            criteriaBuilder.not(criteriaBuilder.exists(noScope)),
                            criteriaBuilder.exists(matchingScope)
                    )
            );
        };
    }
}
