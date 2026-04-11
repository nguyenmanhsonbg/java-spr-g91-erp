package com.g90.backend.modules.contract.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class ContractEventContractListQuery {

    @Min(value = 1, message = "Page must be at least 1")
    private Integer page = 1;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    private Integer pageSize = 20;

    @NotBlank(message = "eventStatus is required")
    private String eventStatus;

    private String eventType;
    private String keyword;
    private String contractNumber;
    private String customerId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate eventFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate eventTo;
}
