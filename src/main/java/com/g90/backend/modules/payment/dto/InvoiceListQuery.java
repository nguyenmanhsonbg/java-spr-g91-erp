package com.g90.backend.modules.payment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class InvoiceListQuery {

    @Size(max = 255, message = "keyword must not exceed 255 characters")
    private String keyword;

    @Size(max = 50, message = "invoiceNumber must not exceed 50 characters")
    private String invoiceNumber;

    @Size(max = 36, message = "customerId must not exceed 36 characters")
    private String customerId;

    @Size(max = 255, message = "customerName must not exceed 255 characters")
    private String customerName;

    @Size(max = 36, message = "contractId must not exceed 36 characters")
    private String contractId;

    @Size(max = 20, message = "status must not exceed 20 characters")
    private String status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate issueFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate issueTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueTo;

    @Min(value = 1, message = "page must be at least 1")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize must be at least 1")
    @Max(value = 100, message = "pageSize must not exceed 100")
    private Integer pageSize = 20;

    private String sortBy;
    private String sortDir;
}
