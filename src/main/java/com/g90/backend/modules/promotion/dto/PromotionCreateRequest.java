package com.g90.backend.modules.promotion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromotionCreateRequest {

    @NotBlank(message = "Promotion name is required")
    @Size(max = 255, message = "Promotion name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Promotion type is required")
    @Size(max = 50, message = "Promotion type must not exceed 50 characters")
    private String promotionType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Discount value must have up to 10 integer digits and 2 decimal places")
    private BigDecimal discountValue;

    @NotNull(message = "Valid from is required")
    private LocalDate validFrom;

    @NotNull(message = "Valid to is required")
    private LocalDate validTo;

    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status;

    @Min(value = 0, message = "Priority must be at least 0")
    private Integer priority;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private List<@NotBlank(message = "Product id is required") @Size(max = 36, message = "Product id must not exceed 36 characters") String> productIds = new ArrayList<>();

    private List<@NotBlank(message = "Customer group is required") @Size(max = 50, message = "Customer group must not exceed 50 characters") String> customerGroups = new ArrayList<>();
}
