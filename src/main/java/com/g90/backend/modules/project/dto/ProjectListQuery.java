package com.g90.backend.modules.project.dto;

import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectListQuery {

    @Min(value = 1, message = "page must be greater than 0")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize must be greater than 0")
    private Integer pageSize = 20;

    private String projectCode;
    private String projectName;
    private String customerId;
    private String status;
    private String progressStatus;
    private String warehouseId;
    private String assignedManager;
    private Boolean archived;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endTo;

    private String sortBy = "createdAt";
    private String sortDir = "desc";
}
