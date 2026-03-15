package com.g90.backend.modules.contract.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractFormInitQuery {

    @Size(max = 36, message = "Customer ID must not exceed 36 characters")
    private String customerId;

    @Size(max = 36, message = "Quotation ID must not exceed 36 characters")
    private String quotationId;
}
