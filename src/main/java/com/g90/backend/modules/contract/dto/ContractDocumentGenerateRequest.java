package com.g90.backend.modules.contract.dto;

import com.g90.backend.modules.contract.entity.ContractDocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractDocumentGenerateRequest {

    @NotNull(message = "Document type is required")
    private ContractDocumentType documentType;

    private Boolean officialDocument = Boolean.FALSE;
}
