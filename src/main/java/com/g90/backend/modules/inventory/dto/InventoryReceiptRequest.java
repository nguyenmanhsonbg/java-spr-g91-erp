package com.g90.backend.modules.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryReceiptRequest {

    @NotBlank(message = "productId is required")
    private String productId;

    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0.01", message = "quantity must be greater than 0")
    private BigDecimal quantity;

    @NotNull(message = "receiptDate is required")
    private LocalDateTime receiptDate;

    @Size(max = 255, message = "supplierName must not exceed 255 characters")
    private String supplierName;

    @Size(max = 255, message = "reason must not exceed 255 characters")
    private String reason;

    @Size(max = 500, message = "note must not exceed 500 characters")
    private String note;
}
