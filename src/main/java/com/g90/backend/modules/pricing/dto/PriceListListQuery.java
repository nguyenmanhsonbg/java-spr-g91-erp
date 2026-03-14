package com.g90.backend.modules.pricing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PriceListListQuery {

    @Min(value = 0, message = "page must be greater than or equal to 0")
    private Integer page = 0;

    @Min(value = 1, message = "size must be greater than 0")
    private Integer size = 10;

    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status;

    @Size(max = 50, message = "Customer group must not exceed 50 characters")
    private String customerGroup;
}
