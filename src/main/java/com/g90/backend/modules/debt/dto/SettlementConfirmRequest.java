package com.g90.backend.modules.debt.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettlementConfirmRequest {

    @Size(max = 500, message = "note must not exceed 500 characters")
    private String note;

    private Boolean generateCertificate;
}
