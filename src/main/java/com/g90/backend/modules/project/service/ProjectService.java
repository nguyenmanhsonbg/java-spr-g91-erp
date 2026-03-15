package com.g90.backend.modules.project.service;

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
import java.util.List;

public interface ProjectService {

    ProjectResponse createProject(ProjectCreateRequest request);

    ProjectListResponseData getProjects(ProjectListQuery query);

    ProjectDetailResponseData getProjectDetail(String projectId);

    ProjectResponse updateProject(String projectId, ProjectUpdateRequest request);

    ProjectResponse archiveProject(String projectId, ProjectArchiveRequest request);

    ProjectResponse restoreProject(String projectId);

    ProjectResponse assignWarehouses(String projectId, ProjectWarehouseAssignRequest request);

    ProjectProgressResponse addProgressUpdate(String projectId, ProjectProgressRequest request);

    ProjectProgressResponse updateProgressUpdate(String projectId, String progressUpdateId, ProjectProgressRequest request);

    List<ProjectMilestoneResponse> getProjectMilestones(String projectId);

    ProjectMilestoneResponse confirmMilestone(String projectId, String milestoneId);

    ProjectFinancialSummaryResponseData getFinancialSummary(String projectId);

    ProjectResponse closeProject(String projectId, ProjectCloseRequest request);
}
