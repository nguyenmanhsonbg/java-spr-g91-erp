package com.g90.backend.modules.project.mapper;

import com.g90.backend.modules.project.dto.ProjectDetailResponseData;
import com.g90.backend.modules.project.dto.ProjectMilestoneResponse;
import com.g90.backend.modules.project.dto.ProjectProgressResponse;
import com.g90.backend.modules.project.dto.ProjectResponse;
import com.g90.backend.modules.project.entity.ProjectDocumentEntity;
import com.g90.backend.modules.project.entity.ProjectManagementEntity;
import com.g90.backend.modules.project.entity.ProjectMilestoneEntity;
import com.g90.backend.modules.project.entity.ProjectProgressUpdateEntity;
import com.g90.backend.modules.project.entity.ProjectWarehouseAssignmentEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public ProjectResponse toProjectResponse(ProjectManagementEntity entity) {
        String customerName = entity.getCustomer().getCompanyName();
        if (customerName == null || customerName.isBlank()) {
            customerName = entity.getCustomer().getContactPerson();
        }
        return new ProjectResponse(
                entity.getId(),
                entity.getProjectCode(),
                entity.getCustomer().getId(),
                customerName,
                entity.getName(),
                entity.getLocation(),
                entity.getScope(),
                entity.getStatus(),
                entity.getProgressPercent(),
                entity.getProgressStatus(),
                entity.getCurrentPhase(),
                entity.getBudget(),
                entity.getBudgetApprovalStatus(),
                entity.getArchiveApprovalStatus(),
                entity.getAssignedProjectManager(),
                entity.getPrimaryWarehouse() == null ? null : entity.getPrimaryWarehouse().getId(),
                entity.getPrimaryWarehouse() == null ? null : entity.getPrimaryWarehouse().getName(),
                entity.getBackupWarehouse() == null ? null : entity.getBackupWarehouse().getId(),
                entity.getBackupWarehouse() == null ? null : entity.getBackupWarehouse().getName(),
                entity.getLinkedContract() == null ? null : entity.getLinkedContract().getId(),
                entity.getLinkedOrderReference(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getArchivedAt(),
                entity.getRestoreDeadline(),
                entity.getClosedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ProjectMilestoneResponse toMilestoneResponse(ProjectMilestoneEntity entity, boolean autoConfirmEligible) {
        return new ProjectMilestoneResponse(
                entity.getId(),
                entity.getName(),
                entity.getMilestoneType(),
                entity.getCompletionPercent(),
                entity.getAmount(),
                entity.getDueDate(),
                entity.getStatus(),
                entity.getConfirmationStatus(),
                entity.getConfirmed(),
                entity.getConfirmedAt(),
                entity.getConfirmationDeadline(),
                autoConfirmEligible,
                entity.getPaymentReleaseReady(),
                entity.getNotes()
        );
    }

    public ProjectProgressResponse toProgressResponse(ProjectProgressUpdateEntity entity) {
        return new ProjectProgressResponse(
                entity.getId(),
                entity.getProject().getId(),
                entity.getPreviousProgressPercent(),
                entity.getProgressPercent(),
                entity.getProgressStatus(),
                entity.getPhase(),
                entity.getNotes(),
                entity.getChangeReason(),
                entity.getBehindSchedule(),
                entity.getEvidenceCount(),
                entity.getCreatedAt()
        );
    }

    public ProjectDetailResponseData.DocumentData toDocumentResponse(ProjectDocumentEntity entity) {
        return new ProjectDetailResponseData.DocumentData(
                entity.getId(),
                entity.getDocumentType(),
                entity.getFileName(),
                entity.getFileUrl(),
                entity.getContentType(),
                entity.getUploadedAt()
        );
    }

    public ProjectDetailResponseData.WarehouseAssignmentHistory toWarehouseHistory(ProjectWarehouseAssignmentEntity entity) {
        return new ProjectDetailResponseData.WarehouseAssignmentHistory(
                entity.getId(),
                entity.getWarehouse().getId(),
                entity.getWarehouse().getName(),
                entity.getAssignmentType(),
                entity.getActive(),
                entity.getAssignmentReason(),
                entity.getAssignedAt(),
                entity.getEndedAt()
        );
    }

    public List<ProjectMilestoneResponse> toMilestoneResponses(List<ProjectMilestoneEntity> entities) {
        return entities.stream()
                .map(entity -> toMilestoneResponse(entity, false))
                .toList();
    }
}
