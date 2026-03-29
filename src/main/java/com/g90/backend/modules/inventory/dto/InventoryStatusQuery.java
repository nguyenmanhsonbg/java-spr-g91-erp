package com.g90.backend.modules.inventory.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryStatusQuery {

    @Min(value = 1, message = "page must be greater than 0")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize must be greater than 0")
    private Integer pageSize = 20;

    private String search;
    private String productId;
}
