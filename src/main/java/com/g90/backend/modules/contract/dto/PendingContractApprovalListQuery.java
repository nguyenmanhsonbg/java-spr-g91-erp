package com.g90.backend.modules.contract.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class PendingContractApprovalListQuery {

    private String keyword;
    private String customerId;
    private String pendingAction;
    private String approvalTier;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate requestedFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate requestedTo;

    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum amount must be at least 0")
    private BigDecimal minAmount;

    @DecimalMin(value = "0.0", inclusive = true, message = "Maximum amount must be at least 0")
    private BigDecimal maxAmount;

    @Min(value = 1, message = "Page must be at least 1")
    private Integer page = 1;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    private Integer pageSize = 20;

    private String sortBy;
    private String sortDir;
}
