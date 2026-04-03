package com.g90.backend.modules.debt.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentAllocationRequest {

    @NotBlank(message = "invoiceId is required")
    private String invoiceId;

    @DecimalMin(value = "0.01", message = "allocatedAmount must be greater than 0")
    private BigDecimal allocatedAmount;
}
