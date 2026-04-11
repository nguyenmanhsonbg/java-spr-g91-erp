package com.g90.backend.modules.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateContractFromQuotationRequest {

    @NotBlank(message = "Payment terms is required")
    @Size(max = 255, message = "Payment terms must not exceed 255 characters")
    private String paymentTerms;

    @Size(max = 20, message = "Payment option code must not exceed 20 characters")
    private String paymentOptionCode;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 500, message = "Delivery address must not exceed 500 characters")
    private String deliveryAddress;
}
