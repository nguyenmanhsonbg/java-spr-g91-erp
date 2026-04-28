package com.g90.backend.modules.contract.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractCancellationSettlementConfirmRequest {

    @NotNull(message = "paidAmount is required")
    @DecimalMin(value = "0.01", message = "paidAmount must be greater than 0")
    @Digits(integer = 18, fraction = 2, message = "paidAmount must be a valid monetary value")
    private BigDecimal paidAmount;

    @NotBlank(message = "paymentMethod is required")
    @Size(max = 50, message = "paymentMethod must not exceed 50 characters")
    private String paymentMethod;

    @Size(max = 100, message = "referenceNo must not exceed 100 characters")
    private String referenceNo;

    @Size(max = 1000, message = "proofDocumentUrl must not exceed 1000 characters")
    private String proofDocumentUrl;

    @Size(max = 1000, message = "note must not exceed 1000 characters")
    private String note;

    private LocalDateTime paidAt;
}
