package com.g90.backend.modules.product.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductListQuery {

    @Min(value = 1, message = "page must be greater than 0")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize must be greater than 0")
    private Integer pageSize = 20;

    private String keyword;
    private String search;
    private String type;
    private String size;
    private String sizeValue;
    private String thickness;
    private String unit;
    private String status;
    private String sortBy = "createdAt";
    private String sortDir = "desc";
}
