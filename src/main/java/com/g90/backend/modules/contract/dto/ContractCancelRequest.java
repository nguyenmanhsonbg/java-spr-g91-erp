package com.g90.backend.modules.contract.dto;

import com.g90.backend.modules.contract.entity.ContractCancellationReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractCancelRequest {

    @NotNull(message = "Cancellation reason is required")
    private ContractCancellationReason cancellationReason;

    @Size(max = 1000, message = "Cancellation note must not exceed 1000 characters")
    private String cancellationNote;
}
