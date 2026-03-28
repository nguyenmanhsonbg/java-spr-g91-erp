package com.g90.backend.modules.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryAdjustmentRequest {

    @NotBlank(message = "productId is required")
    private String productId;

    @NotNull(message = "adjustmentQuantity is required")
    private BigDecimal adjustmentQuantity;

    @NotBlank(message = "reason is required")
    @Size(max = 255, message = "reason must not exceed 255 characters")
    private String reason;

    @Size(max = 500, message = "note must not exceed 500 characters")
    private String note;
}
