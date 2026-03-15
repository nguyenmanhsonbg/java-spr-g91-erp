package com.g90.backend.modules.project.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectDetailResponseData(
        ProjectResponse project,
        Timeline timeline,
        ProjectFinancialSummaryResponseData financialSummary,
        List<ProjectMilestoneResponse> milestones,
        List<DocumentData> documents,
        List<OrderReferenceData> associatedOrders,
        List<DeliveryHistoryData> deliveryHistory,
        PaymentStatus paymentStatus,
        List<ProjectProgressResponse> progressUpdates,
        Warehouses warehouses
) {

    public record Timeline(
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime lastProgressUpdateAt,
            Boolean weeklyUpdateOverdue,
            Boolean behindSchedule
    ) {
    }

    public record DocumentData(
            String id,
            String documentType,
            String fileName,
            String fileUrl,
            String contentType,
            LocalDateTime uploadedAt
    ) {
    }

    public record OrderReferenceData(
            String reference,
            String status
    ) {
    }

    public record DeliveryHistoryData(
            String reference,
            String status,
            String note
    ) {
    }

    public record PaymentStatus(
            String status,
            java.math.BigDecimal paymentsReceived,
            java.math.BigDecimal paymentsDue,
            java.math.BigDecimal outstandingBalance
    ) {
    }

    public record Warehouses(
            WarehouseData primary,
            WarehouseData backup,
            List<WarehouseAssignmentHistory> history
    ) {
    }

    public record WarehouseData(
            String id,
            String name,
            String location
    ) {
    }

    public record WarehouseAssignmentHistory(
            String id,
            String warehouseId,
            String warehouseName,
            String assignmentType,
            Boolean active,
            String assignmentReason,
            LocalDateTime assignedAt,
            LocalDateTime endedAt
    ) {
    }
}
