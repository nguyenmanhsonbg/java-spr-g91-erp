package com.g90.backend.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectWarehouseAssignRequest {

    @NotBlank(message = "Primary warehouse ID is required")
    @Size(max = 36, message = "Primary warehouse ID must not exceed 36 characters")
    private String primaryWarehouseId;

    @Size(max = 36, message = "Backup warehouse ID must not exceed 36 characters")
    private String backupWarehouseId;

    @Size(max = 1000, message = "Assignment reason must not exceed 1000 characters")
    private String assignmentReason;
}
