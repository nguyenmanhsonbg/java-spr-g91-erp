package com.g90.backend.modules.pricing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PriceListUpdateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Applicable customer group is required")
    @Size(max = 50, message = "Applicable customer group must not exceed 50 characters")
    private String customerGroup;

    @NotNull(message = "Valid from is required")
    private LocalDate validFrom;

    @NotNull(message = "Valid to is required")
    private LocalDate validTo;

    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status;

    @Valid
    @NotEmpty(message = "At least one pricing item is required")
    private List<PriceListItemWriteRequest> items;
}
