package com.g90.backend.modules.contract.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractCancellationSettlementQuery {

    @Size(max = 36, message = "contractId must not exceed 36 characters")
    private String contractId;

    @Size(max = 36, message = "customerId must not exceed 36 characters")
    private String customerId;

    @Size(max = 40, message = "settlementType must not exceed 40 characters")
    private String settlementType;

    @Size(max = 20, message = "status must not exceed 20 characters")
    private String status;

    @Min(value = 1, message = "page must be at least 1")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize must be at least 1")
    @Max(value = 100, message = "pageSize must not exceed 100")
    private Integer pageSize = 20;
}
