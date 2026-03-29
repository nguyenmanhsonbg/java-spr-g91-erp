package com.g90.backend.modules.inventory.dto;

import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryHistoryQuery {

    @Min(value = 1, message = "page must be greater than 0")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize must be greater than 0")
    private Integer pageSize = 20;

    private String productId;
    private String transactionType;
    private LocalDate fromDate;
    private LocalDate toDate;
}
