package com.g90.backend.modules.payment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceUpdateRequest {

    private LocalDate issueDate;

    private LocalDate dueDate;

    @Digits(integer = 18, fraction = 2, message = "adjustmentAmount must be a valid monetary value")
    private BigDecimal adjustmentAmount;

    @Size(max = 500, message = "billingAddress must not exceed 500 characters")
    private String billingAddress;

    @Size(max = 255, message = "paymentTerms must not exceed 255 characters")
    private String paymentTerms;

    @Size(max = 1000, message = "note must not exceed 1000 characters")
    private String note;

    @Size(max = 20, message = "status must not exceed 20 characters")
    private String status;

    @Size(max = 1000, message = "documentUrl must not exceed 1000 characters")
    private String documentUrl;

    @Valid
    private List<InvoiceItemRequest> items;
}
