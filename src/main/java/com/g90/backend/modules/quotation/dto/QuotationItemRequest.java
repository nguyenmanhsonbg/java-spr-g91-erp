package com.g90.backend.modules.quotation.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuotationItemRequest {

    @JsonIgnore
    private final Set<String> unexpectedFields = new LinkedHashSet<>();

    @NotBlank(message = "Product is required")
    @Size(max = 36, message = "Product ID must not exceed 36 characters")
    private String productId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @JsonAnySetter
    public void captureUnexpectedField(String fieldName, Object ignoredValue) {
        unexpectedFields.add(fieldName);
    }
}
