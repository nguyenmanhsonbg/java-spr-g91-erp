package com.g90.backend.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectArchiveRequest {

    @NotBlank(message = "Archive reason is required")
    @Size(max = 1000, message = "Archive reason must not exceed 1000 characters")
    private String reason;
}
