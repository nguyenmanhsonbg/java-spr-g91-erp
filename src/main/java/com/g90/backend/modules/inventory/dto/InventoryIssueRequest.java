package com.g90.backend.modules.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryIssueRequest {

    @NotBlank(message = "productId is required")
    private String productId;

    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0.01", message = "quantity must be greater than 0")
    private BigDecimal quantity;

    @Size(max = 36, message = "relatedOrderId must not exceed 36 characters")
    private String relatedOrderId;

    @Size(max = 36, message = "relatedProjectId must not exceed 36 characters")
    private String relatedProjectId;

    @Size(max = 255, message = "reason must not exceed 255 characters")
    private String reason;

    @Size(max = 500, message = "note must not exceed 500 characters")
    private String note;
}
