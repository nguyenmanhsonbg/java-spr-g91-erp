package com.g90.backend.modules.project.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectMilestoneRequest {

    @NotBlank(message = "Milestone name is required")
    @Size(max = 255, message = "Milestone name must not exceed 255 characters")
    private String name;

    @Size(max = 30, message = "Milestone type must not exceed 30 characters")
    private String milestoneType = "PAYMENT";

    @NotNull(message = "Completion percent is required")
    @Min(value = 1, message = "Completion percent must be at least 1")
    @Max(value = 100, message = "Completion percent must not exceed 100")
    private Integer completionPercent;

    @DecimalMin(value = "0.0", inclusive = true, message = "Amount must be greater than or equal to 0")
    private BigDecimal amount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    @Size(max = 1000, message = "Milestone notes must not exceed 1000 characters")
    private String notes;
}
