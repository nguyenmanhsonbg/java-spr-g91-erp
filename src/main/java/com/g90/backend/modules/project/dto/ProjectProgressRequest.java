package com.g90.backend.modules.project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectProgressRequest {

    @NotNull(message = "Progress percent is required")
    @Min(value = 0, message = "Progress percent must be between 0 and 100")
    @Max(value = 100, message = "Progress percent must be between 0 and 100")
    private Integer progressPercent;

    @Size(max = 30, message = "Progress status must not exceed 30 characters")
    private String progressStatus;

    @Size(max = 50, message = "Phase must not exceed 50 characters")
    private String phase;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @Size(max = 1000, message = "Change reason must not exceed 1000 characters")
    private String changeReason;

    @Valid
    private List<ProjectDocumentMetadataRequest> evidenceDocuments;
}
