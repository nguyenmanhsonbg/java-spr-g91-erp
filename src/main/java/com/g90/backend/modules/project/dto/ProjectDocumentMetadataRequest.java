package com.g90.backend.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectDocumentMetadataRequest {

    @Size(max = 30, message = "Document type must not exceed 30 characters")
    private String documentType = "DOCUMENT";

    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name must not exceed 255 characters")
    private String fileName;

    @NotBlank(message = "File URL is required")
    @Size(max = 500, message = "File URL must not exceed 500 characters")
    private String fileUrl;

    @Size(max = 100, message = "Content type must not exceed 100 characters")
    private String contentType;
}
