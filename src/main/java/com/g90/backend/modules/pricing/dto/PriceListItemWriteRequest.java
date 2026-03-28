package com.g90.backend.modules.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PriceListItemWriteRequest {

    @Size(max = 36, message = "Item ID must not exceed 36 characters")
    private String id;

    @NotBlank(message = "Product is required")
    @Size(max = 36, message = "Product ID must not exceed 36 characters")
    private String productId;

    @NotNull(message = "Unit price in VND is required")
    @DecimalMin(value = "0.01", message = "Unit price in VND must be greater than 0")
    private BigDecimal unitPriceVnd;

    @Size(max = 30, message = "Pricing rule type must not exceed 30 characters")
    private String pricingRuleType;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
}
