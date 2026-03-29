package com.g90.backend.modules.product.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductListQuery {

    @Min(value = 1, message = "page must be greater than or equal to 1")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize must be greater than or equal to 1")
    private Integer pageSize;

    private String keyword;
    private String search;
    private String type;
    @Parameter(hidden = true)
    private String size;
    private String sizeValue;
    private String thickness;
    private String unit;
    private String status;
    private String sortBy = "createdAt";
    private String sortDir = "desc";
}
