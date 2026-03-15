package com.g90.backend.modules.project.repository;

import com.g90.backend.modules.project.dto.ProjectListQuery;
import com.g90.backend.modules.project.entity.ProjectManagementEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProjectSpecifications {

    private ProjectSpecifications() {
    }

    public static Specification<ProjectManagementEntity> withFilters(ProjectListQuery query) {
        return Specification.where(like("projectCode", query.getProjectCode()))
                .and(like("name", query.getProjectName()))
                .and(equalsNested("customer", "id", query.getCustomerId()))
                .and(equalsIgnoreCase("status", query.getStatus()))
                .and(equalsIgnoreCase("progressStatus", query.getProgressStatus()))
                .and(warehouseEquals(query.getWarehouseId()))
                .and(like("assignedProjectManager", query.getAssignedManager()))
                .and(archived(query.getArchived()))
                .and(createdFrom(query.getCreatedFrom()))
                .and(createdTo(query.getCreatedTo()))
                .and(dateFrom("startDate", query.getStartFrom()))
                .and(dateTo("startDate", query.getStartTo()))
                .and(dateFrom("endDate", query.getEndFrom()))
                .and(dateTo("endDate", query.getEndTo()));
    }

    private static Specification<ProjectManagementEntity> like(String field, String value) {
        return (root, cq, cb) -> {
            if (!StringUtils.hasText(value)) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get(field)), "%" + value.trim().toLowerCase() + "%");
        };
    }

    private static Specification<ProjectManagementEntity> equalsIgnoreCase(String field, String value) {
        return (root, cq, cb) -> {
            if (!StringUtils.hasText(value)) {
                return cb.conjunction();
            }
            return cb.equal(cb.upper(root.get(field)), value.trim().toUpperCase());
        };
    }

    private static Specification<ProjectManagementEntity> equalsNested(String parent, String field, String value) {
        return (root, cq, cb) -> {
            if (!StringUtils.hasText(value)) {
                return cb.conjunction();
            }
            return cb.equal(root.get(parent).get(field), value.trim());
        };
    }

    private static Specification<ProjectManagementEntity> warehouseEquals(String warehouseId) {
        return (root, cq, cb) -> {
            if (!StringUtils.hasText(warehouseId)) {
                return cb.conjunction();
            }
            return cb.or(
                    cb.equal(root.get("primaryWarehouse").get("id"), warehouseId.trim()),
                    cb.equal(root.get("backupWarehouse").get("id"), warehouseId.trim())
            );
        };
    }

    private static Specification<ProjectManagementEntity> archived(Boolean archived) {
        return (root, cq, cb) -> {
            if (archived == null) {
                return cb.conjunction();
            }
            return archived ? cb.isNotNull(root.get("archivedAt")) : cb.isNull(root.get("archivedAt"));
        };
    }

    private static Specification<ProjectManagementEntity> createdFrom(LocalDate date) {
        return (root, cq, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("createdAt"), date.atStartOfDay());
        };
    }

    private static Specification<ProjectManagementEntity> createdTo(LocalDate date) {
        return (root, cq, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
            return cb.lessThanOrEqualTo(root.get("createdAt"), end);
        };
    }

    private static Specification<ProjectManagementEntity> dateFrom(String field, LocalDate date) {
        return (root, cq, cb) -> date == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get(field), date);
    }

    private static Specification<ProjectManagementEntity> dateTo(String field, LocalDate date) {
        return (root, cq, cb) -> date == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get(field), date);
    }
}
