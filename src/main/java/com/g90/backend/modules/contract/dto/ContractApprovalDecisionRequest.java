package com.g90.backend.modules.contract.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractApprovalDecisionRequest {

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
}
