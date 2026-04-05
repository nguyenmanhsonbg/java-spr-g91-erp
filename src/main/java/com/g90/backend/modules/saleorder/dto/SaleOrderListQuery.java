package com.g90.backend.modules.saleorder.dto;

import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaleOrderListQuery {

    @Min(value = 1, message = "page must be greater than 0")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize must be greater than 0")
    private Integer pageSize = 20;

    private String keyword;
    private String saleOrderNumber;
    private String contractNumber;
    private String customerId;
    private String projectId;
    private String status;
    private LocalDate orderFrom;
    private LocalDate orderTo;
    private LocalDate deliveryFrom;
    private LocalDate deliveryTo;
    private String sortBy;
    private String sortDir;
}
