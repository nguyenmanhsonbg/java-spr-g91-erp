package com.g90.backend.modules.account.repository;

import com.g90.backend.modules.account.dto.AccountListQuery;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class AccountSpecifications {

    private AccountSpecifications() {
    }

    public static Specification<UserAccountEntity> withFilters(AccountListQuery query) {
        return Specification.where(internalAccountsOnly())
                .and(roleEquals(query.getRole()))
                .and(statusEquals(query.getStatus()));
    }

    private static Specification<UserAccountEntity> internalAccountsOnly() {
        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.notEqual(criteriaBuilder.upper(root.join("role").get("name")), "CUSTOMER");
    }

    private static Specification<UserAccountEntity> roleEquals(String role) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(role)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                    criteriaBuilder.upper(root.join("role").get("name")),
                    role.trim().toUpperCase()
            );
        };
    }

    private static Specification<UserAccountEntity> statusEquals(String status) {
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
}
