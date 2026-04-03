package com.g90.backend.modules.debt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCreateRequest {

    @NotBlank(message = "customerId is required")
    private String customerId;

    @NotNull(message = "paymentDate is required")
    private LocalDate paymentDate;

    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "paymentMethod is required")
    @Size(max = 50, message = "paymentMethod must not exceed 50 characters")
    private String paymentMethod;

    @Size(max = 100, message = "referenceNo must not exceed 100 characters")
    private String referenceNo;

    @Size(max = 500, message = "note must not exceed 500 characters")
    private String note;

    @Valid
    private List<PaymentAllocationRequest> allocations;
}
