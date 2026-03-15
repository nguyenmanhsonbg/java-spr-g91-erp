package com.g90.backend.modules.contract.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractItemRequest {

    @NotBlank(message = "Product ID is required")
    @Size(max = 36, message = "Product ID must not exceed 36 characters")
    private String productId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    private BigDecimal unitPrice;

    @Size(max = 500, message = "Price override reason must not exceed 500 characters")
    private String priceOverrideReason;
}
