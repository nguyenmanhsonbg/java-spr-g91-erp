package com.g90.backend.modules.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceItemRequest {

    @Size(max = 36, message = "productId must not exceed 36 characters")
    private String productId;

    @Size(max = 255, message = "description must not exceed 255 characters")
    private String description;

    @Size(max = 20, message = "unit must not exceed 20 characters")
    private String unit;

    @Digits(integer = 18, fraction = 2, message = "quantity must be a valid monetary value")
    @DecimalMin(value = "0.01", message = "quantity must be greater than 0")
    private BigDecimal quantity;

    @Digits(integer = 18, fraction = 2, message = "unitPrice must be a valid monetary value")
    @DecimalMin(value = "0.01", message = "unitPrice must be greater than 0")
    private BigDecimal unitPrice;
}
