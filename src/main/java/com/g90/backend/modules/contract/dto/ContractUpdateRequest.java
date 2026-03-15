package com.g90.backend.modules.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractUpdateRequest extends ContractPreviewRequest {

    @NotBlank(message = "Change reason is required")
    @Size(max = 500, message = "Change reason must not exceed 500 characters")
    private String changeReason;
}
