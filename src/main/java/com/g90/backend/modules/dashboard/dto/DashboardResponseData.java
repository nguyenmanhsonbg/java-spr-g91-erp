package com.g90.backend.modules.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponseData(
        String role,
        LocalDateTime generatedAt,
        Summary summary,
        List<PendingApprovalItem> pendingApprovals,
        List<PaymentConfirmationItem> pendingPaymentConfirmations,
        List<OverdueInvoiceItem> overdueInvoices,
        List<WarehouseActionItem> warehouseActions,
        List<MilestoneConfirmationItem> milestoneConfirmations
) {

    public record Summary(
            long pendingApprovalCount,
            long pendingPaymentConfirmationCount,
            long overdueInvoiceCount,
            BigDecimal overdueAmount,
            long warehouseActionCount,
            long milestoneConfirmationCount
    ) {
    }

    public record PendingApprovalItem(
            String approvalId,
            String contractId,
            String contractNumber,
            String customerId,
            String customerName,
            BigDecimal totalAmount,
            String approvalType,
            String approvalTier,
            String pendingAction,
            String requestedBy,
            LocalDateTime requestedAt,
            LocalDateTime dueAt,
            String status
    ) {
    }

    public record PaymentConfirmationItem(
            String requestId,
            String invoiceId,
            String invoiceNumber,
            String customerId,
            String customerCode,
            String customerName,
            BigDecimal requestedAmount,
            LocalDateTime transferTime,
            String senderBankName,
            String referenceCode,
            String status,
            LocalDateTime createdAt
    ) {
    }

    public record OverdueInvoiceItem(
            String invoiceId,
            String invoiceNumber,
            String contractId,
            String contractNumber,
            String customerId,
            String customerCode,
            String customerName,
            LocalDate dueDate,
            long overdueDays,
            BigDecimal grandTotal,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,
            String status
    ) {
    }

    public record WarehouseActionItem(
            String contractId,
            String saleOrderNumber,
            String contractNumber,
            String customerId,
            String customerName,
            LocalDate expectedDeliveryDate,
            LocalDate actualDeliveryDate,
            String status,
            BigDecimal totalAmount,
            LocalDateTime submittedAt
    ) {
    }

    public record MilestoneConfirmationItem(
            String milestoneId,
            String projectId,
            String projectCode,
            String projectName,
            String customerId,
            String customerName,
            String milestoneName,
            Integer completionPercent,
            BigDecimal amount,
            LocalDate dueDate,
            LocalDateTime confirmationDeadline,
            String status,
            String confirmationStatus
    ) {
    }
}
