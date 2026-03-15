package com.g90.backend.modules.contract.dto;

import java.time.LocalDateTime;

public record ContractApprovalResponseData(
        String contractId,
        String contractNumber,
        String approvalStatus,
        String contractStatus,
        String decision,
        String decidedBy,
        LocalDateTime decidedAt,
        String comment
) {
}
