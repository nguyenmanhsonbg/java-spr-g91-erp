package com.g90.backend.modules.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectResponse(
        String id,
        String projectCode,
        String customerId,
        String customerName,
        String name,
        String location,
        String scope,
        String status,
        Integer progressPercent,
        String progressStatus,
        String currentPhase,
        BigDecimal budget,
        String budgetApprovalStatus,
        String archiveApprovalStatus,
        String assignedProjectManager,
        String primaryWarehouseId,
        String primaryWarehouseName,
        String backupWarehouseId,
        String backupWarehouseName,
        String linkedContractId,
        String linkedOrderReference,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime archivedAt,
        LocalDateTime restoreDeadline,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
