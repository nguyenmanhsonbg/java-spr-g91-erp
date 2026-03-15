package com.g90.backend.modules.contract.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class ContractListQuery {

    private String keyword;
    private String contractNumber;
    private String customerId;
    private String status;
    private String approvalStatus;
    private Boolean confidential;
    private Boolean submitted;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deliveryFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deliveryTo;

    @Min(value = 1, message = "Page must be at least 1")
    private Integer page = 1;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    private Integer pageSize = 20;

    private String sortBy;
    private String sortDir;
}
