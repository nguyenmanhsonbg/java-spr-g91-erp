package com.g90.backend.modules.contract.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ContractDetailResponseData(
        ContractResponse contract,
        CustomerData customer,
        QuotationData quotation,
        List<ContractItemResponse> items,
        ApprovalData approval,
        CreditData credit,
        List<VersionData> versions,
        List<StatusHistoryData> statusHistory,
        List<DocumentData> documents,
        String confidentialityNote
) {
    public record CustomerData(
            String id,
            String companyName,
            String customerType,
            BigDecimal creditLimit,
            String status
    ) {
    }

    public record QuotationData(
            String id,
            String quotationNumber,
            String status
    ) {
    }

    public record ApprovalData(
            boolean requiresApproval,
            String approvalStatus,
            String approvalTier,
            String pendingAction,
            LocalDateTime requestedAt,
            LocalDateTime dueAt,
            String decidedBy,
            LocalDateTime decidedAt
    ) {
    }

    public record CreditData(
            BigDecimal creditLimit,
            BigDecimal currentDebt,
            BigDecimal projectedDebt,
            BigDecimal availableCredit
    ) {
    }

    public record VersionData(
            Integer versionNo,
            String changeReason,
            String changedBy,
            LocalDateTime createdAt
    ) {
    }

    public record StatusHistoryData(
            String fromStatus,
            String toStatus,
            String changeReason,
            String changedBy,
            LocalDateTime changedAt
    ) {
    }

    public record DocumentData(
            String id,
            String documentType,
            String documentNumber,
            String fileName,
            String fileUrl,
            boolean previewOnly,
            boolean officialDocument,
            String watermarkText,
            Integer exportCount,
            LocalDateTime generatedAt,
            LocalDateTime lastExportedAt,
            LocalDateTime emailedAt
    ) {
    }
}
