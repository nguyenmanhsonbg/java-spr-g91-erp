package com.g90.backend.modules.project.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.project.dto.ProjectArchiveRequest;
import com.g90.backend.modules.project.dto.ProjectCloseRequest;
import com.g90.backend.modules.project.dto.ProjectCreateRequest;
import com.g90.backend.modules.project.dto.ProjectDetailResponseData;
import com.g90.backend.modules.project.dto.ProjectFinancialSummaryResponseData;
import com.g90.backend.modules.project.dto.ProjectListQuery;
import com.g90.backend.modules.project.dto.ProjectListResponseData;
import com.g90.backend.modules.project.dto.ProjectMilestoneResponse;
import com.g90.backend.modules.project.dto.ProjectProgressRequest;
import com.g90.backend.modules.project.dto.ProjectProgressResponse;
import com.g90.backend.modules.project.dto.ProjectResponse;
import com.g90.backend.modules.project.dto.ProjectUpdateRequest;
import com.g90.backend.modules.project.dto.ProjectWarehouseAssignRequest;
import com.g90.backend.modules.project.service.ProjectService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created successfully", projectService.createProject(request)));
    }

    @GetMapping
    public ApiResponse<ProjectListResponseData> getProjects(@Valid @ModelAttribute ProjectListQuery query) {
        return ApiResponse.success("Project list fetched successfully", projectService.getProjects(query));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectDetailResponseData> getProjectDetail(@PathVariable String projectId) {
        return ApiResponse.success("Project detail fetched successfully", projectService.getProjectDetail(projectId));
    }

    @PutMapping("/{projectId}")
    public ApiResponse<ProjectResponse> updateProject(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectUpdateRequest request
    ) {
        return ApiResponse.success("Project updated successfully", projectService.updateProject(projectId, request));
    }

    @PatchMapping("/{projectId}/archive")
    public ApiResponse<ProjectResponse> archiveProject(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectArchiveRequest request
    ) {
        return ApiResponse.success("Project archived successfully", projectService.archiveProject(projectId, request));
    }

    @PostMapping("/{projectId}/restore")
    public ApiResponse<ProjectResponse> restoreProject(@PathVariable String projectId) {
        return ApiResponse.success("Project restored successfully", projectService.restoreProject(projectId));
    }

    @PostMapping("/{projectId}/warehouses")
    public ApiResponse<ProjectResponse> assignWarehouses(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectWarehouseAssignRequest request
    ) {
        return ApiResponse.success("Project warehouse assignment updated successfully", projectService.assignWarehouses(projectId, request));
    }

    @PostMapping("/{projectId}/progress")
    public ResponseEntity<ApiResponse<ProjectProgressResponse>> addProgressUpdate(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectProgressRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project progress updated successfully", projectService.addProgressUpdate(projectId, request)));
    }

    @PutMapping("/{projectId}/progress/{progressUpdateId}")
    public ApiResponse<ProjectProgressResponse> updateProgressUpdate(
            @PathVariable String projectId,
            @PathVariable String progressUpdateId,
            @Valid @RequestBody ProjectProgressRequest request
    ) {
        return ApiResponse.success("Project progress updated successfully", projectService.updateProgressUpdate(projectId, progressUpdateId, request));
    }

    @GetMapping("/{projectId}/milestones")
    public ApiResponse<List<ProjectMilestoneResponse>> getProjectMilestones(@PathVariable String projectId) {
        return ApiResponse.success("Project milestones fetched successfully", projectService.getProjectMilestones(projectId));
    }

    @PostMapping("/{projectId}/milestones/{milestoneId}/confirm")
    public ApiResponse<ProjectMilestoneResponse> confirmMilestone(
            @PathVariable String projectId,
            @PathVariable String milestoneId
    ) {
        return ApiResponse.success("Project milestone confirmed successfully", projectService.confirmMilestone(projectId, milestoneId));
    }

    @GetMapping("/{projectId}/financial-summary")
    public ApiResponse<ProjectFinancialSummaryResponseData> getFinancialSummary(@PathVariable String projectId) {
        return ApiResponse.success("Project financial summary fetched successfully", projectService.getFinancialSummary(projectId));
    }

    @PostMapping("/{projectId}/close")
    public ApiResponse<ProjectResponse> closeProject(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectCloseRequest request
    ) {
        return ApiResponse.success("Project closed successfully", projectService.closeProject(projectId, request));
    }
}
