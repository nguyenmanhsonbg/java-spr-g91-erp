package com.g90.backend.modules.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductCreateRequest {

    @NotBlank(message = "Product code is required")
    @Size(max = 50, message = "productCode must not exceed 50 characters")
    private String productCode;

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "productName must not exceed 255 characters")
    private String productName;

    @NotBlank(message = "Type is required")
    @Size(max = 100, message = "type must not exceed 100 characters")
    private String type;

    @NotBlank(message = "Size is required")
    @Size(max = 100, message = "size must not exceed 100 characters")
    private String size;

    @NotBlank(message = "Thickness is required")
    @Size(max = 50, message = "thickness must not exceed 50 characters")
    private String thickness;

    @NotBlank(message = "Unit is required")
    @Size(max = 20, message = "unit must not exceed 20 characters")
    private String unit;

    @DecimalMin(value = "0.0", message = "weightConversion must be greater than or equal to 0")
    private BigDecimal weightConversion;

    @DecimalMin(value = "0.0", message = "referenceWeight must be greater than or equal to 0")
    private BigDecimal referenceWeight;

    @Size(max = 1000, message = "description must not exceed 1000 characters")
    private String description;

    private String status;

    private List<
            @NotBlank(message = "imageUrls must not contain blank values")
            @Size(max = 500, message = "Each image URL must not exceed 500 characters")
            String> imageUrls;
}
