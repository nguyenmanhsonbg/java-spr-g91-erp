package com.g90.backend.modules.project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectUpdateRequest {

    @Size(max = 255, message = "Project name must not exceed 255 characters")
    private String name;

    @Size(max = 500, message = "Project location must not exceed 500 characters")
    private String location;

    @Size(max = 1000, message = "Project scope must not exceed 1000 characters")
    private String scope;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @DecimalMin(value = "0.0", inclusive = false, message = "Budget must be greater than 0")
    private BigDecimal budget;

    @Size(max = 255, message = "Assigned project manager must not exceed 255 characters")
    private String assignedProjectManager;

    @Size(max = 36, message = "Primary warehouse ID must not exceed 36 characters")
    private String primaryWarehouseId;

    @Size(max = 36, message = "Backup warehouse ID must not exceed 36 characters")
    private String backupWarehouseId;

    @Size(max = 36, message = "Linked contract ID must not exceed 36 characters")
    private String linkedContractId;

    @Size(max = 100, message = "Linked order reference must not exceed 100 characters")
    private String linkedOrderReference;

    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status;

    @DecimalMin(value = "0.0", inclusive = true, message = "Actual spend must be greater than or equal to 0")
    private BigDecimal actualSpend;

    @DecimalMin(value = "0.0", inclusive = true, message = "Commitments must be greater than or equal to 0")
    private BigDecimal commitments;

    @DecimalMin(value = "0.0", inclusive = true, message = "Payments received must be greater than or equal to 0")
    private BigDecimal paymentsReceived;

    @DecimalMin(value = "0.0", inclusive = true, message = "Payments due must be greater than or equal to 0")
    private BigDecimal paymentsDue;

    @DecimalMin(value = "0.0", inclusive = true, message = "Outstanding balance must be greater than or equal to 0")
    private BigDecimal outstandingBalance;

    private Integer openOrderCount;
    private Integer unresolvedIssueCount;
    private Boolean customerSignoffCompleted;

    @Valid
    @Size(min = 3, message = "At least 3 payment milestones are required")
    private List<ProjectMilestoneRequest> paymentMilestones;

    @NotBlank(message = "Change reason is required")
    @Size(max = 1000, message = "Change reason must not exceed 1000 characters")
    private String changeReason;
}
