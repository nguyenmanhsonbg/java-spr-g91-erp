package com.g90.backend.modules.project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.CustomerProfileNotFoundException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.ProjectArchiveNotAllowedException;
import com.g90.backend.exception.ProjectCloseNotAllowedException;
import com.g90.backend.exception.ProjectMilestoneNotFoundException;
import com.g90.backend.exception.ProjectNotFoundException;
import com.g90.backend.exception.ProjectProgressUpdateNotFoundException;
import com.g90.backend.exception.ProjectRestoreNotAllowedException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.contract.repository.ContractRepository;
import com.g90.backend.modules.product.dto.PaginationResponse;
import com.g90.backend.modules.project.dto.ProjectArchiveRequest;
import com.g90.backend.modules.project.dto.ProjectCloseRequest;
import com.g90.backend.modules.project.dto.ProjectCreateRequest;
import com.g90.backend.modules.project.dto.ProjectDetailResponseData;
import com.g90.backend.modules.project.dto.ProjectDocumentMetadataRequest;
import com.g90.backend.modules.project.dto.ProjectFinancialSummaryResponseData;
import com.g90.backend.modules.project.dto.ProjectListQuery;
import com.g90.backend.modules.project.dto.ProjectListResponseData;
import com.g90.backend.modules.project.dto.ProjectMilestoneRequest;
import com.g90.backend.modules.project.dto.ProjectMilestoneResponse;
import com.g90.backend.modules.project.dto.ProjectProgressRequest;
import com.g90.backend.modules.project.dto.ProjectProgressResponse;
import com.g90.backend.modules.project.dto.ProjectResponse;
import com.g90.backend.modules.project.dto.ProjectUpdateRequest;
import com.g90.backend.modules.project.dto.ProjectWarehouseAssignRequest;
import com.g90.backend.modules.project.entity.ProjectApprovalStatus;
import com.g90.backend.modules.project.entity.ProjectDocumentEntity;
import com.g90.backend.modules.project.entity.ProjectDocumentType;
import com.g90.backend.modules.project.entity.ProjectInvoiceEntity;
import com.g90.backend.modules.project.entity.ProjectManagementEntity;
import com.g90.backend.modules.project.entity.ProjectMilestoneEntity;
import com.g90.backend.modules.project.entity.ProjectMilestoneStatus;
import com.g90.backend.modules.project.entity.ProjectProgressStatus;
import com.g90.backend.modules.project.entity.ProjectProgressUpdateEntity;
import com.g90.backend.modules.project.entity.ProjectStatus;
import com.g90.backend.modules.project.entity.ProjectWarehouseAssignmentEntity;
import com.g90.backend.modules.project.entity.ProjectWarehouseAssignmentType;
import com.g90.backend.modules.project.entity.WarehouseEntity;
import com.g90.backend.modules.project.mapper.ProjectMapper;
import com.g90.backend.modules.project.repository.ProjectDocumentRepository;
import com.g90.backend.modules.project.repository.ProjectInvoiceRepository;
import com.g90.backend.modules.project.repository.ProjectMilestoneRepository;
import com.g90.backend.modules.project.repository.ProjectProgressUpdateRepository;
import com.g90.backend.modules.project.repository.ProjectManagementRepository;
import com.g90.backend.modules.project.repository.ProjectSpecifications;
import com.g90.backend.modules.project.repository.ProjectWarehouseAssignmentRepository;
import com.g90.backend.modules.project.repository.WarehouseRepository;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal TEN_PERCENT = new BigDecimal("1.10");
    private static final Set<String> FINAL_CONTRACT_STATUSES = Set.of("COMPLETED", "CLOSED", "CANCELLED", "VOID", "TERMINATED");
    private static final Set<String> FINAL_INVOICE_STATUSES = Set.of("PAID", "SETTLED", "CLOSED", "CANCELLED", "VOID");
    private static final Set<String> PROJECT_SORT_FIELDS = Set.of("createdAt", "projectCode", "name", "status", "progressPercent", "startDate", "endDate");
    private static final Set<String> CLOSABLE_MILESTONE_STATUSES = Set.of(
            ProjectMilestoneStatus.CONFIRMED.name(),
            ProjectMilestoneStatus.AUTO_CONFIRMED.name(),
            ProjectMilestoneStatus.COMPLETED.name()
    );

    private final ProjectManagementRepository projectRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final ProjectProgressUpdateRepository projectProgressUpdateRepository;
    private final ProjectWarehouseAssignmentRepository projectWarehouseAssignmentRepository;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final ProjectInvoiceRepository projectInvoiceRepository;
    private final WarehouseRepository warehouseRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ContractRepository contractRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogRepository auditLogRepository;
    private final ProjectMapper projectMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request) {
        UserAccountEntity currentUser = requireAccountantAccess();
        validateDateRange(request.getStartDate(), request.getEndDate(), "startDate", "endDate");
        validateMilestoneRequests(request.getPaymentMilestones());

        CustomerProfileEntity customer = customerProfileRepository.findById(request.getCustomerId())
                .orElseThrow(CustomerProfileNotFoundException::new);
        WarehouseEntity primaryWarehouse = resolveWarehouse(request.getPrimaryWarehouseId(), "primaryWarehouseId");
        WarehouseEntity backupWarehouse = resolveWarehouse(request.getBackupWarehouseId(), "backupWarehouseId");
        validateWarehouses(primaryWarehouse, backupWarehouse);
        ContractEntity linkedContract = resolveLinkedContract(request.getLinkedContractId(), customer.getId());

        ProjectManagementEntity project = new ProjectManagementEntity();
        project.setProjectCode(generateProjectCode());
        project.setCustomer(customer);
        project.setName(normalizeRequired(request.getName(), "name", "Project name is required"));
        project.setLocation(normalizeRequired(request.getLocation(), "location", "Project location is required"));
        project.setScope(normalizeNullable(request.getScope()));
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setBudget(normalizeMoney(request.getBudget()));
        project.setAssignedProjectManager(normalizeRequired(
                request.getAssignedProjectManager(),
                "assignedProjectManager",
                "Assigned project manager is required"
        ));
        project.setPrimaryWarehouse(primaryWarehouse);
        project.setBackupWarehouse(backupWarehouse);
        project.setLinkedContract(linkedContract);
        project.setLinkedOrderReference(normalizeNullable(request.getLinkedOrderReference()));
        project.setStatus(resolveCreateStatus(request.getStatus()).name());
        project.setProgressPercent(0);
        project.setProgressStatus(ProjectProgressStatus.ON_TRACK.name());
        project.setBudgetApprovalStatus(ProjectApprovalStatus.NOT_REQUIRED.name());
        project.setArchiveApprovalStatus(ProjectApprovalStatus.NOT_REQUIRED.name());
        project.setCreatedBy(currentUser.getId());
        project.setUpdatedBy(currentUser.getId());
        project.setMilestones(buildMilestones(project, request.getPaymentMilestones()));

        ProjectManagementEntity saved = projectRepository.save(project);
        replaceWarehouseAssignments(saved, primaryWarehouse, backupWarehouse, "Initial project assignment", currentUser.getId());
        ProjectResponse response = projectMapper.toProjectResponse(saved);
        logAudit("CREATE_PROJECT", saved.getId(), null, response, currentUser.getId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectListResponseData getProjects(ProjectListQuery query) {
        UserAccountEntity currentUser = loadCurrentUserEntity();
        validateListQuery(query);

        if (RoleName.CUSTOMER == roleOf(currentUser)) {
            CustomerProfileEntity customerProfile = loadCurrentCustomerProfile();
            if (StringUtils.hasText(query.getCustomerId()) && !customerProfile.getId().equals(query.getCustomerId().trim())) {
                throw new ForbiddenOperationException("Customers can only view their own projects");
            }
            query.setCustomerId(customerProfile.getId());
        }

        int pageNumber = normalizePage(query.getPage());
        int pageSize = normalizePageSize(query.getPageSize());
        Page<ProjectManagementEntity> page = projectRepository.findAll(
                ProjectSpecifications.withFilters(query),
                PageRequest.of(pageNumber - 1, pageSize, buildSort(query))
        );

        return new ProjectListResponseData(
                page.getContent().stream().map(projectMapper::toProjectResponse).toList(),
                PaginationResponse.builder()
                        .page(pageNumber)
                        .pageSize(pageSize)
                        .totalItems(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build(),
                new ProjectListResponseData.Filters(
                        query.getProjectCode(),
                        query.getProjectName(),
                        query.getCustomerId(),
                        query.getStatus(),
                        query.getProgressStatus(),
                        query.getWarehouseId(),
                        query.getAssignedManager(),
                        query.getArchived(),
                        query.getCreatedFrom(),
                        query.getCreatedTo(),
                        query.getStartFrom(),
                        query.getStartTo(),
                        query.getEndFrom(),
                        query.getEndTo()
                )
        );
    }

    @Override
    @Transactional
    public ProjectDetailResponseData getProjectDetail(String projectId) {
        ProjectManagementEntity project = loadAccessibleProject(projectId);
        refreshAutoConfirmations(project);
        return buildProjectDetail(project);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(String projectId, ProjectUpdateRequest request) {
        UserAccountEntity currentUser = requireAccountantAccess();
        ProjectManagementEntity project = loadProjectForAccountant(projectId);
        ensureProjectUpdatable(project);

        LocalDate nextStartDate = request.getStartDate() != null ? request.getStartDate() : project.getStartDate();
        LocalDate nextEndDate = request.getEndDate() != null ? request.getEndDate() : project.getEndDate();
        validateDateRange(nextStartDate, nextEndDate, "startDate", "endDate");

        ProjectResponse oldState = projectMapper.toProjectResponse(project);

        if (request.getName() != null) {
            project.setName(normalizeRequired(request.getName(), "name", "Project name must not be blank"));
        }
        if (request.getLocation() != null) {
            project.setLocation(normalizeRequired(request.getLocation(), "location", "Project location must not be blank"));
        }
        if (request.getScope() != null) {
            project.setScope(normalizeNullable(request.getScope()));
        }
        if (request.getStartDate() != null) {
            project.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            project.setEndDate(request.getEndDate());
        }
        if (request.getAssignedProjectManager() != null) {
            project.setAssignedProjectManager(normalizeRequired(
                    request.getAssignedProjectManager(),
                    "assignedProjectManager",
                    "Assigned project manager must not be blank"
            ));
        }
        if (request.getLinkedOrderReference() != null) {
            project.setLinkedOrderReference(normalizeNullable(request.getLinkedOrderReference()));
        }
        if (request.getLinkedContractId() != null) {
            project.setLinkedContract(resolveLinkedContract(request.getLinkedContractId(), project.getCustomer().getId()));
        }
        if (request.getStatus() != null) {
            ProjectStatus status = resolveUpdateStatus(request.getStatus());
            if (status == ProjectStatus.COMPLETED && effectiveProgressPercent(project, request) < 100) {
                throw RequestValidationException.singleError("status", "Project cannot be marked COMPLETED before progress reaches 100%");
            }
            project.setStatus(status.name());
        }

        if (request.getActualSpend() != null) {
            project.setActualSpend(normalizeMoney(request.getActualSpend()));
        }
        if (request.getCommitments() != null) {
            project.setCommitments(normalizeMoney(request.getCommitments()));
        }
        if (request.getPaymentsReceived() != null) {
            project.setPaymentsReceived(normalizeMoney(request.getPaymentsReceived()));
        }
        if (request.getPaymentsDue() != null) {
            project.setPaymentsDue(normalizeMoney(request.getPaymentsDue()));
        }
        if (request.getOutstandingBalance() != null) {
            project.setOutstandingBalance(normalizeMoney(request.getOutstandingBalance()));
        }
        if (request.getOpenOrderCount() != null) {
            project.setOpenOrderCount(request.getOpenOrderCount());
        }
        if (request.getUnresolvedIssueCount() != null) {
            project.setUnresolvedIssueCount(request.getUnresolvedIssueCount());
        }
        if (request.getCustomerSignoffCompleted() != null) {
            project.setCustomerSignoffCompleted(request.getCustomerSignoffCompleted());
            if (Boolean.TRUE.equals(request.getCustomerSignoffCompleted()) && project.getCustomerSignoffAt() == null) {
                project.setCustomerSignoffAt(LocalDateTime.now(APP_ZONE));
            }
        }

        if (request.getBudget() != null) {
            BigDecimal oldBudget = project.getBudget();
            BigDecimal newBudget = normalizeMoney(request.getBudget());
            validateBudgetAgainstCommitments(newBudget, project.getActualSpend(), project.getCommitments());
            project.setBudget(newBudget);
            if (newBudget.compareTo(oldBudget.multiply(TEN_PERCENT).setScale(2, RoundingMode.HALF_UP)) > 0) {
                project.setBudgetApprovalStatus(ProjectApprovalStatus.APPROVAL_READY.name());
            } else {
                project.setBudgetApprovalStatus(ProjectApprovalStatus.NOT_REQUIRED.name());
            }
        }

        if (request.getPaymentMilestones() != null) {
            validateMilestoneRequests(request.getPaymentMilestones());
            ensureMilestonesEditable(project);
            replaceMilestones(project, request.getPaymentMilestones());
        }

        WarehouseEntity primaryWarehouse = project.getPrimaryWarehouse();
        WarehouseEntity backupWarehouse = project.getBackupWarehouse();
        boolean warehouseChangeRequested = false;
        if (request.getPrimaryWarehouseId() != null) {
            primaryWarehouse = resolveWarehouse(request.getPrimaryWarehouseId(), "primaryWarehouseId");
            warehouseChangeRequested = true;
        }
        if (request.getBackupWarehouseId() != null) {
            backupWarehouse = resolveWarehouse(request.getBackupWarehouseId(), "backupWarehouseId");
            warehouseChangeRequested = true;
        }
        if (warehouseChangeRequested) {
            validateWarehouses(primaryWarehouse, backupWarehouse);
            replaceWarehouseAssignments(project, primaryWarehouse, backupWarehouse, request.getChangeReason(), currentUser.getId());
        }

        if (project.getStatus().equals(ProjectStatus.ACTIVE.name()) && project.getProgressPercent() >= 100) {
            project.setStatus(ProjectStatus.COMPLETED.name());
        }

        project.setUpdatedBy(currentUser.getId());
        ProjectManagementEntity saved = projectRepository.save(project);
        ProjectResponse response = projectMapper.toProjectResponse(saved);
        logAudit("UPDATE_PROJECT", saved.getId(), oldState, response, currentUser.getId());
        return response;
    }

    @Override
    @Transactional
    public ProjectResponse archiveProject(String projectId, ProjectArchiveRequest request) {
        UserAccountEntity currentUser = requireAccountantAccess();
        ProjectManagementEntity project = loadProjectForAccountant(projectId);

        if (ProjectStatus.ARCHIVED.name().equals(project.getStatus())) {
            throw RequestValidationException.singleError("status", "Project is already archived");
        }
        ensureArchiveAllowed(project);

        ProjectResponse oldState = projectMapper.toProjectResponse(project);
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        project.setStatus(ProjectStatus.ARCHIVED.name());
        project.setArchivedAt(now);
        project.setArchivedBy(currentUser.getId());
        project.setArchiveReason(normalizeRequired(request.getReason(), "reason", "Archive reason is required"));
        project.setRestoreDeadline(now.plusDays(30));
        project.setArchiveApprovalStatus(ProjectApprovalStatus.APPROVAL_READY.name());
        project.setUpdatedBy(currentUser.getId());

        ProjectManagementEntity saved = projectRepository.save(project);
        ProjectResponse response = projectMapper.toProjectResponse(saved);
        logAudit("ARCHIVE_PROJECT", saved.getId(), oldState, response, currentUser.getId());
        return response;
    }

    @Override
    @Transactional
    public ProjectResponse restoreProject(String projectId) {
        UserAccountEntity currentUser = requireAccountantAccess();
        ProjectManagementEntity project = loadProjectForAccountant(projectId);
        LocalDateTime now = LocalDateTime.now(APP_ZONE);

        if (!ProjectStatus.ARCHIVED.name().equals(project.getStatus())
                || project.getRestoreDeadline() == null
                || now.isAfter(project.getRestoreDeadline())) {
            throw new ProjectRestoreNotAllowedException();
        }

        ProjectResponse oldState = projectMapper.toProjectResponse(project);
        project.setStatus(deriveRestoreStatus(project).name());
        project.setArchivedAt(null);
        project.setArchivedBy(null);
        project.setArchiveReason(null);
        project.setRestoreDeadline(null);
        project.setArchiveApprovalStatus(ProjectApprovalStatus.NOT_REQUIRED.name());
        project.setUpdatedBy(currentUser.getId());

        ProjectManagementEntity saved = projectRepository.save(project);
        ProjectResponse response = projectMapper.toProjectResponse(saved);
        logAudit("RESTORE_PROJECT", saved.getId(), oldState, response, currentUser.getId());
        return response;
    }

    @Override
    @Transactional
    public ProjectResponse assignWarehouses(String projectId, ProjectWarehouseAssignRequest request) {
        UserAccountEntity currentUser = requireAccountantAccess();
        ProjectManagementEntity project = loadProjectForAccountant(projectId);
        ensureProjectUpdatable(project);

        WarehouseEntity primaryWarehouse = resolveWarehouse(request.getPrimaryWarehouseId(), "primaryWarehouseId");
        WarehouseEntity backupWarehouse = resolveWarehouse(request.getBackupWarehouseId(), "backupWarehouseId");
        validateWarehouses(primaryWarehouse, backupWarehouse);

        ProjectResponse oldState = projectMapper.toProjectResponse(project);
        replaceWarehouseAssignments(project, primaryWarehouse, backupWarehouse, request.getAssignmentReason(), currentUser.getId());
        project.setUpdatedBy(currentUser.getId());

        ProjectManagementEntity saved = projectRepository.save(project);
        ProjectResponse response = projectMapper.toProjectResponse(saved);
        logAudit("ASSIGN_PROJECT_WAREHOUSE", saved.getId(), oldState, response, currentUser.getId());
        return response;
    }

    @Override
    @Transactional
    public ProjectProgressResponse addProgressUpdate(String projectId, ProjectProgressRequest request) {
        UserAccountEntity currentUser = requireAccountantAccess();
        ProjectManagementEntity project = loadProjectForAccountant(projectId);
        ensureProjectUpdatable(project);

        validateProgressRequest(project.getProgressPercent(), request, "progressPercent");
        boolean triggersMilestone = triggersMilestone(project, request.getProgressPercent());
        if (triggersMilestone && (request.getEvidenceDocuments() == null || request.getEvidenceDocuments().isEmpty())) {
            throw RequestValidationException.singleError(
                    "evidenceDocuments",
                    "Evidence documents are required when progress triggers milestone confirmation"
            );
        }

        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        ProjectProgressStatus progressStatus = resolveProgressStatus(project, request);
        boolean behindSchedule = isBehindSchedule(project, request.getProgressPercent(), progressStatus);

        ProjectProgressUpdateEntity progressUpdate = new ProjectProgressUpdateEntity();
        progressUpdate.setProject(project);
        progressUpdate.setPreviousProgressPercent(project.getProgressPercent());
        progressUpdate.setProgressPercent(request.getProgressPercent());
        progressUpdate.setProgressStatus(progressStatus.name());
        progressUpdate.setPhase(normalizeNullable(request.getPhase()));
        progressUpdate.setNotes(normalizeNullable(request.getNotes()));
        progressUpdate.setChangeReason(normalizeNullable(request.getChangeReason()));
        progressUpdate.setBehindSchedule(behindSchedule);
        progressUpdate.setCreatedBy(currentUser.getId());

        ProjectProgressUpdateEntity savedProgress = projectProgressUpdateRepository.save(progressUpdate);
        int evidenceCount = attachEvidenceDocuments(project, savedProgress, request.getEvidenceDocuments(), currentUser.getId(), false);
        savedProgress.setEvidenceCount(evidenceCount);

        project.setProgressPercent(request.getProgressPercent());
        project.setProgressStatus(progressStatus.name());
        project.setCurrentPhase(normalizeNullable(request.getPhase()));
        project.setLastProgressUpdateAt(now);
        project.setLastProgressNote(normalizeNullable(request.getNotes()));
        project.setUpdatedBy(currentUser.getId());
        if (request.getProgressPercent() >= 100) {
            project.setStatus(ProjectStatus.COMPLETED.name());
        } else if (ProjectStatus.DRAFT.name().equals(project.getStatus())) {
            project.setStatus(ProjectStatus.ACTIVE.name());
        }

        updateMilestonesForProgress(project, request.getProgressPercent(), now);
        projectRepository.save(project);

        ProjectProgressResponse response = projectMapper.toProgressResponse(savedProgress);
        logAudit("ADD_PROJECT_PROGRESS", project.getId(), null, response, currentUser.getId());
        return response;
    }

    @Override
    @Transactional
    public ProjectProgressResponse updateProgressUpdate(String projectId, String progressUpdateId, ProjectProgressRequest request) {
        UserAccountEntity currentUser = requireAccountantAccess();
        ProjectManagementEntity project = loadProjectForAccountant(projectId);
        ensureProjectUpdatable(project);

        ProjectProgressUpdateEntity progressUpdate = projectProgressUpdateRepository.findByIdAndProject_Id(progressUpdateId, projectId)
                .orElseThrow(ProjectProgressUpdateNotFoundException::new);
        ProjectProgressUpdateEntity latestProgressUpdate = projectProgressUpdateRepository.findByProject_IdOrderByCreatedAtDesc(projectId).stream()
                .findFirst()
                .orElseThrow(ProjectProgressUpdateNotFoundException::new);
        if (!Objects.equals(latestProgressUpdate.getId(), progressUpdateId)) {
            throw RequestValidationException.singleError("progressUpdateId", "Only the latest progress update can be edited");
        }

        validateProgressRequest(progressUpdate.getPreviousProgressPercent(), request, "progressPercent");
        boolean triggersMilestone = triggersMilestone(project, request.getProgressPercent());
        List<ProjectDocumentEntity> existingDocuments = projectDocumentRepository.findByProgressUpdate_Id(progressUpdateId);
        if (triggersMilestone && (request.getEvidenceDocuments() == null || request.getEvidenceDocuments().isEmpty()) && existingDocuments.isEmpty()) {
            throw RequestValidationException.singleError(
                    "evidenceDocuments",
                    "Evidence documents are required when progress triggers milestone confirmation"
            );
        }

        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        ProjectProgressStatus progressStatus = resolveProgressStatus(project, request);
        boolean behindSchedule = isBehindSchedule(project, request.getProgressPercent(), progressStatus);

        ProjectProgressResponse oldState = projectMapper.toProgressResponse(progressUpdate);
        progressUpdate.setProgressPercent(request.getProgressPercent());
        progressUpdate.setProgressStatus(progressStatus.name());
        progressUpdate.setPhase(normalizeNullable(request.getPhase()));
        progressUpdate.setNotes(normalizeNullable(request.getNotes()));
        progressUpdate.setChangeReason(normalizeNullable(request.getChangeReason()));
        progressUpdate.setBehindSchedule(behindSchedule);

        int evidenceCount = request.getEvidenceDocuments() == null
                ? existingDocuments.size()
                : attachEvidenceDocuments(project, progressUpdate, request.getEvidenceDocuments(), currentUser.getId(), true);
        progressUpdate.setEvidenceCount(evidenceCount);
        ProjectProgressUpdateEntity savedProgress = projectProgressUpdateRepository.save(progressUpdate);

        project.setProgressPercent(request.getProgressPercent());
        project.setProgressStatus(progressStatus.name());
        project.setCurrentPhase(normalizeNullable(request.getPhase()));
        project.setLastProgressUpdateAt(now);
        project.setLastProgressNote(normalizeNullable(request.getNotes()));
        project.setUpdatedBy(currentUser.getId());
        if (request.getProgressPercent() >= 100) {
            project.setStatus(ProjectStatus.COMPLETED.name());
        }
        updateMilestonesForProgress(project, request.getProgressPercent(), now);
        projectRepository.save(project);

        ProjectProgressResponse response = projectMapper.toProgressResponse(savedProgress);
        logAudit("UPDATE_PROJECT_PROGRESS", project.getId(), oldState, response, currentUser.getId());
        return response;
    }

    @Override
    @Transactional
    public List<ProjectMilestoneResponse> getProjectMilestones(String projectId) {
        ProjectManagementEntity project = loadAccessibleProject(projectId);
        refreshAutoConfirmations(project);
        return projectMilestoneRepository.findByProject_IdOrderByCompletionPercentAsc(projectId).stream()
                .map(this::toMilestoneResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProjectMilestoneResponse confirmMilestone(String projectId, String milestoneId) {
        CustomerProfileEntity customerProfile = requireCustomerAccess();
        ProjectManagementEntity project = loadProjectForCustomer(projectId);
        refreshAutoConfirmations(project);

        ProjectMilestoneEntity milestone = projectMilestoneRepository.findByIdAndProject_Id(milestoneId, projectId)
                .orElseThrow(ProjectMilestoneNotFoundException::new);

        if (!project.getCustomer().getId().equals(customerProfile.getId())) {
            throw new ForbiddenOperationException("Customers can only confirm milestones for their own projects");
        }

        if (CLOSABLE_MILESTONE_STATUSES.contains(milestone.getStatus())) {
            return toMilestoneResponse(milestone);
        }
        if (!ProjectMilestoneStatus.READY_FOR_CONFIRMATION.name().equals(milestone.getStatus())) {
            throw RequestValidationException.singleError("milestoneId", "Milestone is not ready for customer confirmation");
        }

        ProjectMilestoneResponse oldState = toMilestoneResponse(milestone);
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        milestone.setStatus(ProjectMilestoneStatus.CONFIRMED.name());
        milestone.setConfirmationStatus(ProjectMilestoneStatus.CONFIRMED.name());
        milestone.setConfirmed(Boolean.TRUE);
        milestone.setConfirmedAt(now);
        milestone.setConfirmedByCustomer(customerProfile);
        milestone.setPaymentReleaseReady(Boolean.TRUE);
        ProjectMilestoneEntity saved = projectMilestoneRepository.save(milestone);

        ProjectMilestoneResponse response = toMilestoneResponse(saved);
        logAudit("CONFIRM_PROJECT_MILESTONE", projectId, oldState, response, getCurrentUser().userId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectFinancialSummaryResponseData getFinancialSummary(String projectId) {
        requireAccountantAccess();
        ProjectManagementEntity project = loadProjectForAccountant(projectId);
        return buildFinancialSummary(project);
    }

    @Override
    @Transactional
    public ProjectResponse closeProject(String projectId, ProjectCloseRequest request) {
        UserAccountEntity currentUser = requireAccountantAccess();
        ProjectManagementEntity project = loadProjectForAccountant(projectId);

        if (ProjectStatus.ARCHIVED.name().equals(project.getStatus())) {
            throw new ProjectCloseNotAllowedException("Archived project cannot be closed");
        }
        if (ProjectStatus.CLOSED.name().equals(project.getStatus())) {
            throw new ProjectCloseNotAllowedException("Project is already closed");
        }

        refreshAutoConfirmations(project);
        validateCloseRequest(request);
        ensureCloseAllowed(project, request);

        ProjectResponse oldState = projectMapper.toProjectResponse(project);
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        project.setStatus(ProjectStatus.CLOSED.name());
        project.setClosedAt(now);
        project.setClosedBy(currentUser.getId());
        project.setCloseReason(normalizeRequired(request.getCloseReason(), "closeReason", "Close reason is required"));
        project.setCustomerSignoffCompleted(Boolean.TRUE);
        if (project.getCustomerSignoffAt() == null) {
            project.setCustomerSignoffAt(now);
        }
        project.setCustomerSatisfactionScore(request.getCustomerSatisfactionScore());
        project.setWarrantyStartDate(request.getWarrantyStartDate() != null ? request.getWarrantyStartDate() : now.toLocalDate());
        project.setWarrantyEndDate(request.getWarrantyEndDate());
        project.setUpdatedBy(currentUser.getId());

        ProjectManagementEntity saved = projectRepository.save(project);
        ProjectResponse response = projectMapper.toProjectResponse(saved);
        logAudit("CLOSE_PROJECT", saved.getId(), oldState, response, currentUser.getId());
        return response;
    }

    private AuthenticatedUser getCurrentUser() {
        return currentUserProvider.getCurrentUser();
    }

    private UserAccountEntity loadCurrentUserEntity() {
        return userAccountRepository.findWithRoleById(getCurrentUser().userId())
                .orElseThrow(() -> new ForbiddenOperationException("You do not have permission to perform this action"));
    }

    private CustomerProfileEntity loadCurrentCustomerProfile() {
        return customerProfileRepository.findByUser_Id(getCurrentUser().userId())
                .orElseThrow(CustomerProfileNotFoundException::new);
    }

    private UserAccountEntity requireAccountantAccess() {
        UserAccountEntity currentUser = loadCurrentUserEntity();
        RoleName role = roleOf(currentUser);
        if (role != RoleName.ACCOUNTANT && role != RoleName.OWNER) {
            throw new ForbiddenOperationException("Only accountant users can perform this action");
        }
        return currentUser;
    }

    private CustomerProfileEntity requireCustomerAccess() {
        UserAccountEntity currentUser = loadCurrentUserEntity();
        if (roleOf(currentUser) != RoleName.CUSTOMER) {
            throw new ForbiddenOperationException("Only customer users can perform this action");
        }
        return loadCurrentCustomerProfile();
    }

    private ProjectManagementEntity loadAccessibleProject(String projectId) {
        UserAccountEntity currentUser = loadCurrentUserEntity();
        return switch (roleOf(currentUser)) {
            case ACCOUNTANT, OWNER -> loadProjectForAccountant(projectId);
            case CUSTOMER -> loadProjectForCustomer(projectId);
            default -> throw new ForbiddenOperationException("You do not have permission to access this project");
        };
    }

    private ProjectManagementEntity loadProjectForAccountant(String projectId) {
        return projectRepository.findDetailedById(projectId)
                .orElseThrow(ProjectNotFoundException::new);
    }

    private ProjectManagementEntity loadProjectForCustomer(String projectId) {
        ProjectManagementEntity project = projectRepository.findDetailedById(projectId)
                .orElseThrow(ProjectNotFoundException::new);
        CustomerProfileEntity customerProfile = loadCurrentCustomerProfile();
        if (!project.getCustomer().getId().equals(customerProfile.getId())) {
            throw new ForbiddenOperationException("Customers can only access their own projects");
        }
        return project;
    }

    private void ensureProjectUpdatable(ProjectManagementEntity project) {
        if (ProjectStatus.CLOSED.name().equals(project.getStatus()) || ProjectStatus.ARCHIVED.name().equals(project.getStatus())) {
            throw RequestValidationException.singleError("status", "Closed or archived project cannot be modified");
        }
    }

    private void validateListQuery(ProjectListQuery query) {
        validateDateRangeOptional(query.getCreatedFrom(), query.getCreatedTo(), "createdFrom", "createdTo");
        validateDateRangeOptional(query.getStartFrom(), query.getStartTo(), "startFrom", "startTo");
        validateDateRangeOptional(query.getEndFrom(), query.getEndTo(), "endFrom", "endTo");
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate, String startField, String endField) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw RequestValidationException.singleError(startField, startField + " must be before or equal to " + endField);
        }
    }

    private void validateDateRangeOptional(LocalDate from, LocalDate to, String fromField, String toField) {
        if (from != null && to != null && from.isAfter(to)) {
            throw RequestValidationException.singleError(fromField, fromField + " must be before or equal to " + toField);
        }
    }

    private void validateMilestoneRequests(List<ProjectMilestoneRequest> milestoneRequests) {
        if (milestoneRequests == null || milestoneRequests.size() < 3) {
            throw RequestValidationException.singleError("paymentMilestones", "At least 3 payment milestones are required");
        }

        Set<Integer> completionPercents = new HashSet<>();
        for (ProjectMilestoneRequest request : milestoneRequests) {
            if (!completionPercents.add(request.getCompletionPercent())) {
                throw RequestValidationException.singleError("paymentMilestones", "Milestone completion percent must be unique");
            }
        }
    }

    private void ensureMilestonesEditable(ProjectManagementEntity project) {
        boolean hasCommittedMilestones = project.getMilestones().stream().anyMatch(milestone ->
                CLOSABLE_MILESTONE_STATUSES.contains(milestone.getStatus())
                        || ProjectMilestoneStatus.READY_FOR_CONFIRMATION.name().equals(milestone.getStatus())
        );
        if (hasCommittedMilestones) {
            throw RequestValidationException.singleError(
                    "paymentMilestones",
                    "Project milestones cannot be replaced after confirmation flow has started"
            );
        }
    }

    private List<ProjectMilestoneEntity> buildMilestones(ProjectManagementEntity project, List<ProjectMilestoneRequest> milestoneRequests) {
        return milestoneRequests.stream()
                .sorted(Comparator.comparing(ProjectMilestoneRequest::getCompletionPercent))
                .map(request -> {
                    ProjectMilestoneEntity milestone = new ProjectMilestoneEntity();
                    milestone.setProject(project);
                    milestone.setName(normalizeRequired(request.getName(), "paymentMilestones.name", "Milestone name is required"));
                    milestone.setMilestoneType(resolveMilestoneType(request.getMilestoneType()));
                    milestone.setCompletionPercent(request.getCompletionPercent());
                    milestone.setAmount(normalizeMoney(request.getAmount()));
                    milestone.setDueDate(request.getDueDate());
                    milestone.setNotes(normalizeNullable(request.getNotes()));
                    milestone.setStatus(ProjectMilestoneStatus.PENDING.name());
                    milestone.setConfirmationStatus(ProjectMilestoneStatus.PENDING.name());
                    milestone.setConfirmed(Boolean.FALSE);
                    milestone.setPaymentReleaseReady(Boolean.FALSE);
                    return milestone;
                })
                .toList();
    }

    private void replaceMilestones(ProjectManagementEntity project, List<ProjectMilestoneRequest> milestoneRequests) {
        List<ProjectMilestoneEntity> newMilestones = new ArrayList<>(buildMilestones(project, milestoneRequests));
        project.getMilestones().clear();
        project.getMilestones().addAll(newMilestones);
    }

    private WarehouseEntity resolveWarehouse(String warehouseId, String fieldName) {
        if (!StringUtils.hasText(warehouseId)) {
            return null;
        }
        return warehouseRepository.findById(warehouseId.trim())
                .orElseThrow(() -> RequestValidationException.singleError(fieldName, "Warehouse not found"));
    }

    private void validateWarehouses(WarehouseEntity primaryWarehouse, WarehouseEntity backupWarehouse) {
        if (primaryWarehouse != null && !StringUtils.hasText(primaryWarehouse.getLocation())) {
            throw RequestValidationException.singleError("primaryWarehouseId", "Primary warehouse is not serviceable");
        }
        if (backupWarehouse != null && !StringUtils.hasText(backupWarehouse.getLocation())) {
            throw RequestValidationException.singleError("backupWarehouseId", "Backup warehouse is not serviceable");
        }
        if (primaryWarehouse != null && backupWarehouse != null && primaryWarehouse.getId().equals(backupWarehouse.getId())) {
            throw RequestValidationException.singleError("backupWarehouseId", "Backup warehouse must be different from primary warehouse");
        }
    }

    private ContractEntity resolveLinkedContract(String contractId, String customerId) {
        if (!StringUtils.hasText(contractId)) {
            return null;
        }
        ContractEntity contract = contractRepository.findById(contractId.trim())
                .orElseThrow(() -> RequestValidationException.singleError("linkedContractId", "Linked contract not found"));
        if (contract.getCustomer() == null || !customerId.equals(contract.getCustomer().getId())) {
            throw RequestValidationException.singleError("linkedContractId", "Linked contract must belong to the same customer");
        }
        return contract;
    }

    private ProjectStatus resolveCreateStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return ProjectStatus.ACTIVE;
        }
        ProjectStatus status = parseProjectStatus(value, "status");
        if (status == ProjectStatus.CLOSED || status == ProjectStatus.ARCHIVED || status == ProjectStatus.COMPLETED) {
            throw RequestValidationException.singleError("status", "New project status must be DRAFT, ACTIVE, or ON_HOLD");
        }
        return status;
    }

    private ProjectStatus resolveUpdateStatus(String value) {
        ProjectStatus status = parseProjectStatus(value, "status");
        if (status == ProjectStatus.CLOSED || status == ProjectStatus.ARCHIVED) {
            throw RequestValidationException.singleError("status", "Use dedicated APIs to close or archive a project");
        }
        return status;
    }

    private ProjectStatus parseProjectStatus(String value, String field) {
        try {
            return ProjectStatus.from(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw RequestValidationException.singleError(field, "Invalid project status");
        }
    }

    private String resolveMilestoneType(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : "PAYMENT";
    }

    private ProjectDocumentType resolveDocumentType(String value) {
        try {
            return StringUtils.hasText(value) ? ProjectDocumentType.from(value) : ProjectDocumentType.DOCUMENT;
        } catch (IllegalArgumentException exception) {
            throw RequestValidationException.singleError("evidenceDocuments", "Invalid document type");
        }
    }

    private ProjectProgressStatus resolveProgressStatus(ProjectManagementEntity project, ProjectProgressRequest request) {
        if (StringUtils.hasText(request.getProgressStatus())) {
            try {
                ProjectProgressStatus status = ProjectProgressStatus.from(request.getProgressStatus());
                if (status == ProjectProgressStatus.COMPLETED && request.getProgressPercent() < 100) {
                    throw RequestValidationException.singleError("progressStatus", "Progress status COMPLETED requires 100% progress");
                }
                return status;
            } catch (IllegalArgumentException exception) {
                throw RequestValidationException.singleError("progressStatus", "Invalid progress status");
            }
        }
        if (request.getProgressPercent() >= 100) {
            return ProjectProgressStatus.COMPLETED;
        }
        if (project.getEndDate() != null && LocalDate.now(APP_ZONE).isAfter(project.getEndDate())) {
            return ProjectProgressStatus.DELAYED;
        }
        return isBehindSchedule(project, request.getProgressPercent(), ProjectProgressStatus.AT_RISK)
                ? ProjectProgressStatus.AT_RISK
                : ProjectProgressStatus.ON_TRACK;
    }

    private void validateProgressRequest(Integer previousProgressPercent, ProjectProgressRequest request, String fieldName) {
        if (request.getProgressPercent() < previousProgressPercent && !StringUtils.hasText(request.getChangeReason())) {
            throw RequestValidationException.singleError("changeReason", "Change reason is required when progress decreases");
        }
        if (request.getProgressPercent() < 0 || request.getProgressPercent() > 100) {
            throw RequestValidationException.singleError(fieldName, "Progress percent must be between 0 and 100");
        }
    }

    private boolean triggersMilestone(ProjectManagementEntity project, int newProgressPercent) {
        return project.getMilestones().stream().anyMatch(milestone ->
                newProgressPercent >= milestone.getCompletionPercent()
                        && ProjectMilestoneStatus.PENDING.name().equals(milestone.getStatus())
        );
    }

    private int attachEvidenceDocuments(
            ProjectManagementEntity project,
            ProjectProgressUpdateEntity progressUpdate,
            List<ProjectDocumentMetadataRequest> evidenceDocuments,
            String userId,
            boolean replaceExisting
    ) {
        if (replaceExisting) {
            List<ProjectDocumentEntity> existingDocuments = projectDocumentRepository.findByProgressUpdate_Id(progressUpdate.getId());
            if (!existingDocuments.isEmpty()) {
                projectDocumentRepository.deleteAll(existingDocuments);
            }
        }

        if (evidenceDocuments == null || evidenceDocuments.isEmpty()) {
            return replaceExisting ? 0 : projectDocumentRepository.findByProgressUpdate_Id(progressUpdate.getId()).size();
        }

        List<ProjectDocumentEntity> documents = evidenceDocuments.stream()
                .map(request -> {
                    ProjectDocumentEntity document = new ProjectDocumentEntity();
                    document.setProject(project);
                    document.setProgressUpdate(progressUpdate);
                    document.setDocumentType(resolveDocumentType(request.getDocumentType()).name());
                    document.setFileName(normalizeRequired(request.getFileName(), "evidenceDocuments.fileName", "File name is required"));
                    document.setFileUrl(normalizeRequired(request.getFileUrl(), "evidenceDocuments.fileUrl", "File URL is required"));
                    document.setContentType(normalizeNullable(request.getContentType()));
                    document.setUploadedBy(userId);
                    return document;
                })
                .toList();
        projectDocumentRepository.saveAll(documents);
        return documents.size();
    }

    private void updateMilestonesForProgress(ProjectManagementEntity project, int progressPercent, LocalDateTime now) {
        for (ProjectMilestoneEntity milestone : project.getMilestones()) {
            if (progressPercent >= milestone.getCompletionPercent()
                    && ProjectMilestoneStatus.PENDING.name().equals(milestone.getStatus())) {
                milestone.setStatus(ProjectMilestoneStatus.READY_FOR_CONFIRMATION.name());
                milestone.setConfirmationStatus(ProjectMilestoneStatus.READY_FOR_CONFIRMATION.name());
                milestone.setCompletedAt(now);
                milestone.setConfirmationDeadline(now.plusDays(7));
            }
        }
    }

    private void refreshAutoConfirmations(ProjectManagementEntity project) {
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        boolean changed = false;
        for (ProjectMilestoneEntity milestone : project.getMilestones()) {
            if (ProjectMilestoneStatus.READY_FOR_CONFIRMATION.name().equals(milestone.getStatus())
                    && milestone.getConfirmationDeadline() != null
                    && !milestone.getConfirmationDeadline().isAfter(now)) {
                milestone.setStatus(ProjectMilestoneStatus.AUTO_CONFIRMED.name());
                milestone.setConfirmationStatus(ProjectMilestoneStatus.AUTO_CONFIRMED.name());
                milestone.setConfirmed(Boolean.TRUE);
                milestone.setConfirmedAt(milestone.getConfirmationDeadline());
                milestone.setPaymentReleaseReady(Boolean.TRUE);
                changed = true;
            }
        }
        if (changed) {
            projectRepository.save(project);
        }
    }

    private ProjectDetailResponseData buildProjectDetail(ProjectManagementEntity project) {
        List<ProjectMilestoneResponse> milestones = projectMilestoneRepository.findByProject_IdOrderByCompletionPercentAsc(project.getId()).stream()
                .map(this::toMilestoneResponse)
                .toList();
        List<ProjectProgressResponse> progressUpdates = projectProgressUpdateRepository.findByProject_IdOrderByCreatedAtDesc(project.getId()).stream()
                .map(projectMapper::toProgressResponse)
                .toList();
        List<ProjectDetailResponseData.DocumentData> documents = projectDocumentRepository.findByProject_IdOrderByUploadedAtDesc(project.getId()).stream()
                .map(projectMapper::toDocumentResponse)
                .toList();
        List<ProjectDetailResponseData.WarehouseAssignmentHistory> warehouseHistory = projectWarehouseAssignmentRepository
                .findByProject_IdOrderByAssignedAtDesc(project.getId()).stream()
                .map(projectMapper::toWarehouseHistory)
                .toList();
        List<ProjectDetailResponseData.OrderReferenceData> associatedOrders = StringUtils.hasText(project.getLinkedOrderReference())
                ? List.of(new ProjectDetailResponseData.OrderReferenceData(
                project.getLinkedOrderReference(),
                project.getOpenOrderCount() > 0 ? "OPEN" : "COMPLETED"
        ))
                : List.of();

        return new ProjectDetailResponseData(
                projectMapper.toProjectResponse(project),
                new ProjectDetailResponseData.Timeline(
                        project.getStartDate(),
                        project.getEndDate(),
                        project.getLastProgressUpdateAt(),
                        isWeeklyUpdateOverdue(project),
                        isBehindSchedule(project, project.getProgressPercent(), safeProgressStatus(project.getProgressStatus()))
                ),
                buildFinancialSummary(project),
                milestones,
                documents,
                associatedOrders,
                List.of(),
                new ProjectDetailResponseData.PaymentStatus(
                        resolvePaymentStatus(project),
                        project.getPaymentsReceived(),
                        project.getPaymentsDue(),
                        project.getOutstandingBalance()
                ),
                progressUpdates,
                new ProjectDetailResponseData.Warehouses(
                        toWarehouseData(project.getPrimaryWarehouse()),
                        toWarehouseData(project.getBackupWarehouse()),
                        warehouseHistory
                )
        );
    }

    private ProjectMilestoneResponse toMilestoneResponse(ProjectMilestoneEntity milestone) {
        boolean autoConfirmEligible = ProjectMilestoneStatus.READY_FOR_CONFIRMATION.name().equals(milestone.getStatus())
                && milestone.getConfirmationDeadline() != null
                && !milestone.getConfirmationDeadline().isAfter(LocalDateTime.now(APP_ZONE));
        return projectMapper.toMilestoneResponse(milestone, autoConfirmEligible);
    }

    private ProjectDetailResponseData.WarehouseData toWarehouseData(WarehouseEntity warehouse) {
        if (warehouse == null) {
            return null;
        }
        return new ProjectDetailResponseData.WarehouseData(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getLocation()
        );
    }

    private ProjectFinancialSummaryResponseData buildFinancialSummary(ProjectManagementEntity project) {
        BigDecimal budget = normalizeMoney(project.getBudget());
        BigDecimal actualSpend = normalizeMoney(project.getActualSpend());
        BigDecimal commitments = normalizeMoney(project.getCommitments());
        BigDecimal paymentsReceived = normalizeMoney(project.getPaymentsReceived());
        BigDecimal paymentsDue = normalizeMoney(project.getPaymentsDue());
        BigDecimal outstandingBalance = normalizeMoney(project.getOutstandingBalance());
        BigDecimal variance = budget.subtract(actualSpend.add(commitments)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal profitabilityAmount = paymentsReceived.subtract(actualSpend).setScale(2, RoundingMode.HALF_UP);
        BigDecimal profitabilityMargin = paymentsReceived.signum() == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : profitabilityAmount.multiply(new BigDecimal("100"))
                .divide(paymentsReceived, 2, RoundingMode.HALF_UP);

        return new ProjectFinancialSummaryResponseData(
                budget,
                actualSpend,
                commitments,
                variance,
                List.of(
                        new ProjectFinancialSummaryResponseData.CategoryBreakdown("ACTUAL_SPEND", actualSpend),
                        new ProjectFinancialSummaryResponseData.CategoryBreakdown("COMMITMENTS", commitments),
                        new ProjectFinancialSummaryResponseData.CategoryBreakdown("PAYMENTS_RECEIVED", paymentsReceived),
                        new ProjectFinancialSummaryResponseData.CategoryBreakdown("PAYMENTS_DUE", paymentsDue),
                        new ProjectFinancialSummaryResponseData.CategoryBreakdown("OUTSTANDING_BALANCE", outstandingBalance)
                ),
                paymentsReceived,
                paymentsDue,
                outstandingBalance,
                profitabilityAmount,
                profitabilityMargin,
                "PROJECT_FIELDS_WITH_FINANCIAL_EXTENSION_POINTS"
        );
    }

    private String resolvePaymentStatus(ProjectManagementEntity project) {
        if (project.getOutstandingBalance() != null && project.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0) {
            return "OUTSTANDING";
        }
        if (project.getPaymentsDue() != null
                && project.getPaymentsDue().compareTo(BigDecimal.ZERO) > 0
                && project.getPaymentsReceived() != null
                && project.getPaymentsReceived().compareTo(project.getPaymentsDue()) >= 0) {
            return "SETTLED";
        }
        if (project.getPaymentsReceived() != null && project.getPaymentsReceived().compareTo(BigDecimal.ZERO) > 0) {
            return "PARTIALLY_PAID";
        }
        return "NOT_STARTED";
    }

    private boolean isWeeklyUpdateOverdue(ProjectManagementEntity project) {
        if (ProjectStatus.CLOSED.name().equals(project.getStatus()) || ProjectStatus.ARCHIVED.name().equals(project.getStatus())) {
            return false;
        }
        LocalDateTime baseline = project.getLastProgressUpdateAt() != null ? project.getLastProgressUpdateAt() : project.getCreatedAt();
        return baseline != null && baseline.plusDays(7).isBefore(LocalDateTime.now(APP_ZONE));
    }

    private boolean isBehindSchedule(ProjectManagementEntity project, int progressPercent, ProjectProgressStatus progressStatus) {
        if (progressPercent >= 100) {
            return false;
        }
        if (progressStatus == ProjectProgressStatus.DELAYED) {
            return true;
        }
        if (project.getStartDate() == null || project.getEndDate() == null || !project.getStartDate().isBefore(project.getEndDate())) {
            return false;
        }

        LocalDate today = LocalDate.now(APP_ZONE);
        if (today.isBefore(project.getStartDate())) {
            return false;
        }

        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(project.getStartDate(), project.getEndDate()) + 1;
        long elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(project.getStartDate(), today.isAfter(project.getEndDate()) ? project.getEndDate() : today) + 1;
        int expectedPercent = (int) Math.floor((elapsedDays * 100.0d) / totalDays);
        if (today.isAfter(project.getEndDate())) {
            return progressPercent < 100;
        }
        return progressPercent + 5 < expectedPercent;
    }

    private void ensureArchiveAllowed(ProjectManagementEntity project) {
        if (project.getLinkedContract() != null || StringUtils.hasText(project.getLinkedOrderReference())) {
            throw new ProjectArchiveNotAllowedException();
        }
        if (project.getOpenOrderCount() != null && project.getOpenOrderCount() > 0) {
            throw new ProjectArchiveNotAllowedException();
        }
        if (hasFinancialActivity(project)) {
            throw new ProjectArchiveNotAllowedException();
        }
    }

    private boolean hasFinancialActivity(ProjectManagementEntity project) {
        return normalizeMoney(project.getActualSpend()).compareTo(BigDecimal.ZERO) > 0
                || normalizeMoney(project.getCommitments()).compareTo(BigDecimal.ZERO) > 0
                || normalizeMoney(project.getPaymentsReceived()).compareTo(BigDecimal.ZERO) > 0
                || normalizeMoney(project.getPaymentsDue()).compareTo(BigDecimal.ZERO) > 0
                || normalizeMoney(project.getOutstandingBalance()).compareTo(BigDecimal.ZERO) > 0;
    }

    private void ensureCloseAllowed(ProjectManagementEntity project, ProjectCloseRequest request) {
        boolean milestonesComplete = project.getMilestones().stream()
                .allMatch(milestone -> CLOSABLE_MILESTONE_STATUSES.contains(milestone.getStatus()));
        if (!milestonesComplete) {
            throw new ProjectCloseNotAllowedException("All milestones must be confirmed before closing");
        }
        if (project.getOpenOrderCount() != null && project.getOpenOrderCount() > 0) {
            throw new ProjectCloseNotAllowedException("Project still has open related orders");
        }
        if (project.getUnresolvedIssueCount() != null && project.getUnresolvedIssueCount() > 0) {
            throw new ProjectCloseNotAllowedException("Project still has unresolved issues");
        }
        if (project.getLinkedContract() != null && !FINAL_CONTRACT_STATUSES.contains(normalizeUpper(project.getLinkedContract().getStatus()))) {
            throw new ProjectCloseNotAllowedException("Project still has an active contract dependency");
        }

        List<ProjectInvoiceEntity> relatedInvoices = loadRelatedInvoices(project);
        boolean hasUnsettledInvoices = relatedInvoices.stream()
                .anyMatch(invoice -> !FINAL_INVOICE_STATUSES.contains(normalizeUpper(invoice.getStatus())));
        if (hasUnsettledInvoices) {
            throw new ProjectCloseNotAllowedException("Project cannot be closed while invoices or payments remain unsettled");
        }

        if (normalizeMoney(project.getOutstandingBalance()).compareTo(BigDecimal.ZERO) > 0
                || normalizeMoney(project.getPaymentsReceived()).compareTo(normalizeMoney(project.getPaymentsDue())) < 0) {
            throw new ProjectCloseNotAllowedException("Project cannot be closed while debts remain outstanding");
        }

        boolean customerSignoffCompleted = Boolean.TRUE.equals(request.getCustomerSignoffCompleted())
                || Boolean.TRUE.equals(project.getCustomerSignoffCompleted());
        if (!customerSignoffCompleted) {
            throw new ProjectCloseNotAllowedException("Customer sign-off is required before closing");
        }
    }

    private List<ProjectInvoiceEntity> loadRelatedInvoices(ProjectManagementEntity project) {
        if (project.getLinkedContract() == null) {
            return List.of();
        }
        return projectInvoiceRepository.findByContractId(project.getLinkedContract().getId());
    }

    private void validateCloseRequest(ProjectCloseRequest request) {
        validateDateRange(request.getWarrantyStartDate(), request.getWarrantyEndDate(), "warrantyStartDate", "warrantyEndDate");
    }

    private void validateBudgetAgainstCommitments(BigDecimal budget, BigDecimal actualSpend, BigDecimal commitments) {
        BigDecimal actual = normalizeMoney(actualSpend);
        BigDecimal committed = normalizeMoney(commitments);
        if (budget.compareTo(actual) < 0 || budget.compareTo(committed) < 0) {
            throw RequestValidationException.singleError("budget", "Budget cannot be lower than actual spend or committed amount");
        }
    }

    private void replaceWarehouseAssignments(
            ProjectManagementEntity project,
            WarehouseEntity primaryWarehouse,
            WarehouseEntity backupWarehouse,
            String reason,
            String userId
    ) {
        if (project.getId() != null) {
            LocalDateTime now = LocalDateTime.now(APP_ZONE);
            List<ProjectWarehouseAssignmentEntity> activeAssignments = projectWarehouseAssignmentRepository.findByProject_IdAndActiveTrue(project.getId());
            for (ProjectWarehouseAssignmentEntity assignment : activeAssignments) {
                assignment.setActive(Boolean.FALSE);
                assignment.setEndedAt(now);
            }
            if (!activeAssignments.isEmpty()) {
                projectWarehouseAssignmentRepository.saveAll(activeAssignments);
            }
        }

        project.setPrimaryWarehouse(primaryWarehouse);
        project.setBackupWarehouse(backupWarehouse);

        if (project.getId() != null) {
            List<ProjectWarehouseAssignmentEntity> newAssignments = new ArrayList<>();
            if (primaryWarehouse != null) {
                newAssignments.add(buildWarehouseAssignment(project, primaryWarehouse, ProjectWarehouseAssignmentType.PRIMARY, reason, userId));
            }
            if (backupWarehouse != null) {
                newAssignments.add(buildWarehouseAssignment(project, backupWarehouse, ProjectWarehouseAssignmentType.BACKUP, reason, userId));
            }
            if (!newAssignments.isEmpty()) {
                projectWarehouseAssignmentRepository.saveAll(newAssignments);
            }
        }
    }

    private ProjectWarehouseAssignmentEntity buildWarehouseAssignment(
            ProjectManagementEntity project,
            WarehouseEntity warehouse,
            ProjectWarehouseAssignmentType assignmentType,
            String reason,
            String userId
    ) {
        ProjectWarehouseAssignmentEntity assignment = new ProjectWarehouseAssignmentEntity();
        assignment.setProject(project);
        assignment.setWarehouse(warehouse);
        assignment.setAssignmentType(assignmentType.name());
        assignment.setAssignmentReason(normalizeNullable(reason));
        assignment.setAssignedBy(userId);
        assignment.setActive(Boolean.TRUE);
        return assignment;
    }

    private ProjectStatus deriveRestoreStatus(ProjectManagementEntity project) {
        if (project.getProgressPercent() != null && project.getProgressPercent() >= 100) {
            return ProjectStatus.COMPLETED;
        }
        return ProjectStatus.ACTIVE;
    }

    private RoleName roleOf(UserAccountEntity user) {
        return RoleName.from(user.getRole().getName());
    }

    private int effectiveProgressPercent(ProjectManagementEntity project, ProjectUpdateRequest request) {
        return project.getProgressPercent() == null ? 0 : project.getProgressPercent();
    }

    private ProjectProgressStatus safeProgressStatus(String status) {
        try {
            return StringUtils.hasText(status) ? ProjectProgressStatus.from(status) : ProjectProgressStatus.ON_TRACK;
        } catch (IllegalArgumentException exception) {
            return ProjectProgressStatus.ON_TRACK;
        }
    }

    private String generateProjectCode() {
        LocalDate yearStartDate = LocalDate.now(APP_ZONE).withDayOfYear(1);
        LocalDateTime start = yearStartDate.atStartOfDay();
        LocalDateTime end = yearStartDate.plusYears(1).atStartOfDay();
        long sequence = projectRepository.countByCreatedAtBetween(start, end) + 1;
        return "PRJ-" + yearStartDate.getYear() + "-" + String.format("%04d", sequence);
    }

    private Sort buildSort(ProjectListQuery query) {
        String sortBy = PROJECT_SORT_FIELDS.contains(query.getSortBy()) ? query.getSortBy() : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sortBy);
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 20 : pageSize;
    }

    private void logAudit(String action, String entityId, Object oldValue, Object newValue, String userId) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType("PROJECT");
        auditLog.setEntityId(entityId);
        auditLog.setOldValue(toJson(oldValue));
        auditLog.setNewValue(toJson(newValue));
        auditLogRepository.save(auditLog);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize audit payload", exception);
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeRequired(String value, String field, String message) {
        if (!StringUtils.hasText(value)) {
            throw RequestValidationException.singleError(field, message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : "";
    }
}
