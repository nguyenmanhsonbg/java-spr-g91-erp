package com.g90.backend.modules.contract.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class ContractPreviewRequest {

    @NotBlank(message = "Customer ID is required")
    @Size(max = 36, message = "Customer ID must not exceed 36 characters")
    private String customerId;

    @Size(max = 36, message = "Quotation ID must not exceed 36 characters")
    private String quotationId;

    @NotBlank(message = "Payment terms are required")
    @Size(max = 255, message = "Payment terms must not exceed 255 characters")
    private String paymentTerms;

    @Size(max = 20, message = "Payment option code must not exceed 20 characters")
    private String paymentOptionCode;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 500, message = "Delivery address must not exceed 500 characters")
    private String deliveryAddress;

    @Size(max = 1000, message = "Delivery terms must not exceed 1000 characters")
    private String deliveryTerms;

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expectedDeliveryDate;

    private Boolean confidential;

    @Valid
    private List<ContractItemRequest> items;
}
