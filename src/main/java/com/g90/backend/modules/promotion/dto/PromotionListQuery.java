package com.g90.backend.modules.promotion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class PromotionListQuery {

    @Min(value = 1, message = "Page must be at least 1")
    private Integer page = 1;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    private Integer pageSize = 20;

    @Size(max = 255, message = "Search must not exceed 255 characters")
    private String search;

    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status;

    @Size(max = 50, message = "Promotion type must not exceed 50 characters")
    private String promotionType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate validFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate validTo;

    @Size(max = 50, message = "Customer group must not exceed 50 characters")
    private String customerGroup;

    @Size(max = 36, message = "Product id must not exceed 36 characters")
    private String productId;
}
