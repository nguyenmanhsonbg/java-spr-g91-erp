package com.g90.backend.modules.contract.dto;

import java.util.List;

public record ContractDocumentListResponseData(
        String contractId,
        String contractNumber,
        List<ContractDetailResponseData.DocumentData> documents
) {
}
