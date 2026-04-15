package com.g90.backend.modules.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentConfirmationRequestConfirmRequest {

    @NotNull(message = "confirmedAmount is required")
    @Digits(integer = 18, fraction = 2, message = "confirmedAmount must be a valid monetary value")
    @DecimalMin(value = "0.01", message = "confirmedAmount must be greater than 0")
    private BigDecimal confirmedAmount;

    @Size(max = 1000, message = "reviewNote must not exceed 1000 characters")
    private String reviewNote;
}
