package com.g90.backend.modules.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceCancelRequest {

    @NotBlank(message = "cancellationReason is required")
    @Size(max = 1000, message = "cancellationReason must not exceed 1000 characters")
    private String cancellationReason;
}
