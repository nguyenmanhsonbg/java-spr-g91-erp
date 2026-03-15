package com.g90.backend.modules.project.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectCloseRequest {

    @NotBlank(message = "Close reason is required")
    @Size(max = 1000, message = "Close reason must not exceed 1000 characters")
    private String closeReason;

    private Boolean customerSignoffCompleted;

    @Min(value = 1, message = "Customer satisfaction score must be between 1 and 5")
    @Max(value = 5, message = "Customer satisfaction score must be between 1 and 5")
    private Integer customerSatisfactionScore;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate warrantyStartDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate warrantyEndDate;
}
