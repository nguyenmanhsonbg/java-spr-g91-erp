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
public class PaymentConfirmationRequestListQuery {

    @Size(max = 255, message = "keyword must not exceed 255 characters")
    private String keyword;

    @Size(max = 36, message = "invoiceId must not exceed 36 characters")
    private String invoiceId;

    @Size(max = 36, message = "customerId must not exceed 36 characters")
    private String customerId;

    @Size(max = 30, message = "status must not exceed 30 characters")
    private String status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdTo;

    @Min(value = 1, message = "page must be at least 1")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize must be at least 1")
    @Max(value = 100, message = "pageSize must not exceed 100")
    private Integer pageSize = 20;

    private String sortBy;
    private String sortDir;
}
