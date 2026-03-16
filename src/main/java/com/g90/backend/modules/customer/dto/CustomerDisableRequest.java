package com.g90.backend.modules.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerDisableRequest {

    @NotBlank(message = "Disable reason is required")
    @Size(max = 1000, message = "Disable reason must not exceed 1000 characters")
    private String reason;
}
