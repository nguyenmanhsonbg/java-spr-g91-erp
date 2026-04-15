package com.g90.backend.modules.payment.dto;

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
public class PaymentConfirmationRequestCreateRequest {

    @NotNull(message = "requestedAmount is required")
    @Digits(integer = 18, fraction = 2, message = "requestedAmount must be a valid monetary value")
    @DecimalMin(value = "0.01", message = "requestedAmount must be greater than 0")
    private BigDecimal requestedAmount;

    @NotNull(message = "transferTime is required")
    private LocalDateTime transferTime;

    @NotBlank(message = "senderBankName is required")
    @Size(max = 255, message = "senderBankName must not exceed 255 characters")
    private String senderBankName;

    @NotBlank(message = "senderAccountName is required")
    @Size(max = 255, message = "senderAccountName must not exceed 255 characters")
    private String senderAccountName;

    @NotBlank(message = "senderAccountNo is required")
    @Size(max = 100, message = "senderAccountNo must not exceed 100 characters")
    private String senderAccountNo;

    @NotBlank(message = "referenceCode is required")
    @Size(max = 100, message = "referenceCode must not exceed 100 characters")
    private String referenceCode;

    @Size(max = 1000, message = "proofDocumentUrl must not exceed 1000 characters")
    private String proofDocumentUrl;

    @Size(max = 1000, message = "note must not exceed 1000 characters")
    private String note;
}
