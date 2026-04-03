package com.g90.backend.modules.debt.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DebtListQuery {

    @Size(max = 100, message = "keyword must not exceed 100 characters")
    private String keyword;

    @Size(max = 50, message = "customerCode must not exceed 50 characters")
    private String customerCode;

    @Size(max = 255, message = "customerName must not exceed 255 characters")
    private String customerName;

    @Size(max = 50, message = "invoiceNumber must not exceed 50 characters")
    private String invoiceNumber;

    @Size(max = 30, message = "status must not exceed 30 characters")
    private String status;

    private Boolean overdueOnly;

    @Min(value = 1, message = "page must be greater than or equal to 1")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize must be greater than or equal to 1")
    @Max(value = 100, message = "pageSize must be less than or equal to 100")
    private Integer pageSize = 20;

    @Size(max = 50, message = "sortBy must not exceed 50 characters")
    private String sortBy;

    @Size(max = 10, message = "sortDir must not exceed 10 characters")
    private String sortDir;
}
