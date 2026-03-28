package com.g90.backend.modules.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Size(max = 20, message = "status must not exceed 20 characters")
    private String status;

    @Size(max = 255, message = "reason must not exceed 255 characters")
    private String reason;

    @Size(max = 50, message = "requestedByRole must not exceed 50 characters")
    private String requestedByRole;
}
