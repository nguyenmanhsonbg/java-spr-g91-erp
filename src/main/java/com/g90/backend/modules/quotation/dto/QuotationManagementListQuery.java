package com.g90.backend.modules.quotation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class QuotationManagementListQuery {

    private String keyword;
    private String quotationNumber;
    private String customerId;
    private String status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    @Min(value = 1, message = "Page must be at least 1")
    private Integer page = 1;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    private Integer pageSize = 20;

    private String sortBy;
    private String sortDir;
}
