package com.g90.backend.modules.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.CustomerProfileNotFoundException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.ProjectArchiveNotAllowedException;
import com.g90.backend.exception.ProjectCloseNotAllowedException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.contract.repository.ContractRepository;
import com.g90.backend.modules.project.dto.ProjectArchiveRequest;
import com.g90.backend.modules.project.dto.ProjectCloseRequest;
import com.g90.backend.modules.project.dto.ProjectCreateRequest;
import com.g90.backend.modules.project.dto.ProjectDocumentMetadataRequest;
import com.g90.backend.modules.project.dto.ProjectMilestoneRequest;
import com.g90.backend.modules.project.dto.ProjectProgressRequest;
import com.g90.backend.modules.project.dto.ProjectUpdateRequest;
import com.g90.backend.modules.project.dto.ProjectWarehouseAssignRequest;
import com.g90.backend.modules.project.entity.ProjectManagementEntity;
import com.g90.backend.modules.project.entity.ProjectMilestoneEntity;
import com.g90.backend.modules.project.entity.ProjectMilestoneStatus;
import com.g90.backend.modules.project.entity.ProjectProgressUpdateEntity;
import com.g90.backend.modules.project.entity.ProjectStatus;
import com.g90.backend.modules.project.entity.WarehouseEntity;
import com.g90.backend.modules.project.mapper.ProjectMapper;
import com.g90.backend.modules.project.repository.ProjectDocumentRepository;
import com.g90.backend.modules.project.repository.ProjectInvoiceRepository;
import com.g90.backend.modules.project.repository.ProjectMilestoneRepository;
import com.g90.backend.modules.project.repository.ProjectProgressUpdateRepository;
import com.g90.backend.modules.project.repository.ProjectManagementRepository;
import com.g90.backend.modules.project.repository.ProjectWarehouseAssignmentRepository;
import com.g90.backend.modules.project.repository.WarehouseRepository;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectServiceImplTest {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private ProjectManagementRepository projectRepository;
    @Mock
    private ProjectMilestoneRepository projectMilestoneRepository;
    @Mock
    private ProjectProgressUpdateRepository projectProgressUpdateRepository;
    @Mock
    private ProjectWarehouseAssignmentRepository projectWarehouseAssignmentRepository;
    @Mock
    private ProjectDocumentRepository projectDocumentRepository;
    @Mock
    private ProjectInvoiceRepository projectInvoiceRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private CustomerProfileRepository customerProfileRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private final ProjectMapper projectMapper = new ProjectMapper();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        projectService = new ProjectServiceImpl(
                projectRepository,
                projectMilestoneRepository,
                projectProgressUpdateRepository,
                projectWarehouseAssignmentRepository,
                projectDocumentRepository,
                projectInvoiceRepository,
                warehouseRepository,
                customerProfileRepository,
                contractRepository,
                userAccountRepository,
                auditLogRepository,
                projectMapper,
                currentUserProvider,
                objectMapper
        );
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectWarehouseAssignmentRepository.findByProject_IdAndActiveTrue(anyString())).thenReturn(List.of());
        when(projectDocumentRepository.findByProgressUpdate_Id(anyString())).thenReturn(List.of());
        when(projectDocumentRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectWarehouseAssignmentRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectMilestoneRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectProgressUpdateRepository.save(any())).thenAnswer(invocation -> {
            ProjectProgressUpdateEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId("progress-1");
            }
            return entity;
        });
        when(projectRepository.save(any())).thenAnswer(invocation -> {
            ProjectManagementEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId("project-1");
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(LocalDateTime.now(APP_ZONE));
            }
            return entity;
        });
    }

    @Test
    void createProjectSuccess() {
        authenticateAs(accountantUser());
        CustomerProfileEntity customer = customerProfile("customer-1", "user-customer-1");
        WarehouseEntity warehouse = warehouse("wh-1");
        ProjectCreateRequest request = createRequest();

        when(projectRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(customerProfileRepository.findById("customer-1")).thenReturn(Optional.of(customer));
        when(warehouseRepository.findById("wh-1")).thenReturn(Optional.of(warehouse));

        var response = projectService.createProject(request);

        assertThat(response.projectCode()).startsWith("PRJ-" + LocalDate.now(APP_ZONE).getYear() + "-");
        assertThat(response.customerId()).isEqualTo("customer-1");
        assertThat(response.primaryWarehouseId()).isEqualTo("wh-1");
    }

    @Test
    void createProjectFailsWhenCustomerMissing() {
        authenticateAs(accountantUser());
        when(customerProfileRepository.findById("customer-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.createProject(createRequest()))
                .isInstanceOf(CustomerProfileNotFoundException.class);
    }

    @Test
    void createProjectFailsWhenMilestonesLessThanThree() {
        authenticateAs(accountantUser());
        ProjectCreateRequest request = createRequest();
        request.setPaymentMilestones(List.of(milestoneRequest("M1", 30), milestoneRequest("M2", 60)));

        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("Invalid request data");
    }

    @Test
    void customerCanViewOwnProject() {
        UserAccountEntity customerUser = customerUser();
        authenticateAs(customerUser);
        CustomerProfileEntity customer = customerProfile("customer-1", customerUser.getId());
        ProjectManagementEntity project = project("project-1", customer, ProjectStatus.ACTIVE.name());
        mockDetailQueries(project);
        when(customerProfileRepository.findByUser_Id(customerUser.getId())).thenReturn(Optional.of(customer));
        when(projectRepository.findDetailedById("project-1")).thenReturn(Optional.of(project));

        var detail = projectService.getProjectDetail("project-1");

        assertThat(detail.project().id()).isEqualTo("project-1");
        assertThat(detail.project().customerId()).isEqualTo("customer-1");
    }

    @Test
    void customerCannotViewAnotherCustomersProject() {
        UserAccountEntity customerUser = customerUser();
        authenticateAs(customerUser);
        CustomerProfileEntity currentCustomer = customerProfile("customer-1", customerUser.getId());
        CustomerProfileEntity otherCustomer = customerProfile("customer-2", "user-customer-2");
        ProjectManagementEntity project = project("project-1", otherCustomer, ProjectStatus.ACTIVE.name());
        when(customerProfileRepository.findByUser_Id(customerUser.getId())).thenReturn(Optional.of(currentCustomer));
        when(projectRepository.findDetailedById("project-1")).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.getProjectDetail("project-1"))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void updateProjectSuccess() {
        authenticateAs(accountantUser());
        ProjectManagementEntity project = project("project-1", customerProfile("customer-1", "user-customer-1"), ProjectStatus.ACTIVE.name());
        when(projectRepository.findDetailedById("project-1")).thenReturn(Optional.of(project));

        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName("Updated Project");
        request.setChangeReason("Scope refinement");

        var response = projectService.updateProject("project-1", request);

        assertThat(response.name()).isEqualTo("Updated Project");
    }

    @Test
    void updateProjectFailsWhenClosed() {
        authenticateAs(accountantUser());
        ProjectManagementEntity project = project("project-1", customerProfile("customer-1", "user-customer-1"), ProjectStatus.CLOSED.name());
        when(projectRepository.findDetailedById("project-1")).thenReturn(Optional.of(project));

        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName("Updated Project");
        request.setChangeReason("Need update");

        assertThatThrownBy(() -> projectService.updateProject("project-1", request))
                .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void budgetIncreaseMoreThanTenPercentTriggersApprovalReady() {
        authenticateAs(accountantUser());
        ProjectManagementEntity project = project("project-1", customerProfile("customer-1", "user-customer-1"), ProjectStatus.ACTIVE.name());
        project.setBudget(new BigDecimal("100.00"));
        when(projectRepository.findDetailedById("project-1")).thenReturn(Optional.of(project));

        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setBudget(new BigDecimal("111.00"));
        request.setChangeReason("Budget extension");

        var response = projectService.updateProject("project-1", request);

        assertThat(response.budgetApprovalStatus()).isEqualTo("APPROVAL_READY");
    }

    @Test
    void archiveProjectBlockedWhenTransactionsExist() {
        authenticateAs(accountantUser());
        ProjectManagementEntity project = project("project-1", customerProfile("customer-1", "user-customer-1"), ProjectStatus.ACTIVE.name());
        project.setActualSpend(new BigDecimal("1.00"));
        when(projectRepository.findDetailedById("project-1")).thenReturn(Optional.of(project));

        ProjectArchiveRequest request = new ProjectArchiveRequest();
        request.setReason("Archive");

        assertThatThrownBy(() -> projectService.archiveProject("project-1", request))
                .isInstanceOf(ProjectArchiveNotAllowedException.class);
    }

    @Test
    void assignWarehouseSuccess() {
        authenticateAs(accountantUser());
        ProjectManagementEntity project = project("project-1", customerProfile("customer-1", "user-customer-1"), ProjectStatus.ACTIVE.name());
        WarehouseEntity primary = warehouse("wh-1");
        WarehouseEntity backup = warehouse("wh-2");
        when(projectRepository.findDetailedById("project-1")).thenReturn(Optional.of(project));
        when(warehouseRepository.findById("wh-1")).thenReturn(Optional.of(primary));
        when(warehouseRepository.findById("wh-2")).thenReturn(Optional.of(backup));

        ProjectWarehouseAssignRequest request = new ProjectWarehouseAssignRequest();
        request.setPrimaryWarehouseId("wh-1");
        request.setBackupWarehouseId("wh-2");
        request.setAssignmentReason("Coverage");

        var response = projectService.assignWarehouses("project-1", request);

        assertThat(response.primaryWarehouseId()).isEqualTo("wh-1");
        assertThat(response.backupWarehouseId()).isEqualTo("wh-2");
    }

    @Test
    void assignWarehouseFailsWhenBackupEqualsPrimary() {
        authenticateAs(accountantUser());
        ProjectManagementEntity project = project("project-1", customerProfile("customer-1", "user-customer-1"), ProjectStatus.ACTIVE.name());
        WarehouseEntity primary = warehouse("wh-1");
        when(projectRepository.findDetailedById("project-1")).thenReturn(Optional.of(project));
        when(warehouseRepository.findById("wh-1")).thenReturn(Optional.of(primary));

        ProjectWarehouseAssignRequest request = new ProjectWarehouseAssignRequest();
        request.setPrimaryWarehouseId("wh-1");
        request.setBackupWarehouseId("wh-1");

        assertThatThrownBy(() -> projectService.assignWarehouses("project-1", request))
                .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void progressUpdateSuccess() {
        authenticateAs(accountantUser());
        ProjectManagementEntity project = project("project-1", customerProfile("customer-1", "user-customer-1"), ProjectStatus.ACTIVE.name());
        project.setProgressPercent(10);
        when(projectRepository.findDetailedById("project-1")).thenReturn(Optional.of(project));

        ProjectProgressRequest request = new ProjectProgressRequest();
        request.setProgressPercent(40);
        request.setPhase("EXECUTION");
        request.setNotes("On track");
        request.setEvidenceDocuments(List.of(documentRequest("progress.jpg")));

        var response = projectService.addProgressUpdate("project-1", request);

        assertThat(response.progressPercent()).isEqualTo(40);
        assertThat(project.getProgressPercent()).isEqualTo(40);
        assertThat(project.getMilestones().get(0).getStatus()).isEqualTo(ProjectMilestoneStatus.READY_FOR_CONFIRMATION.name());
    }

    @Test
    void progressDecreaseWithoutReasonFails() {
        authenticateAs(accountantUser());
        ProjectManagementEntity project = project("project-1", customerProfile("customer-1", "user-customer-1"), ProjectStatus.ACTIVE.name());
        project.setProgressPercent(50);
        when(projectRepository.findDetailedById("project-1")).thenReturn(Optional.of(project));

        ProjectProgressRequest request = new ProjectProgressRequest();
        request.setProgressPercent(40);

        assertThatThrownBy(() -> projectService.addProgressUpdate("project-1", request))
                .isInstanceOf(RequestValidationException.class);
        verify(projectProgressUpdateRepository, never()).save(any());
    }

    @Test
    void milestoneConfirmSuccessByOwnerCustomer() {
        UserAccountEntity customerUser = customerUser();
        authenticateAs(customerUser);
        CustomerProfileEntity customer = customerProfile("customer-1", customerUser.getId());
        ProjectManagementEntity project = project("project-1", customer, ProjectStatus.ACTIVE.name());
        ProjectMilestoneEntity milestone = project.getMilestones().get(0);
        milestone.setStatus(ProjectMilestoneStatus.READY_FOR_CONFIRMATION.name());
        milestone.setConfirmationStatus(ProjectMilestoneStatus.READY_FOR_CONFIRMATION.name());
        milestone.setConfirmationDeadline(LocalDateTime.now(APP_ZONE).plusDays(3));
        when(customerProfileRepository.findByUser_Id(customerUser.getId())).thenReturn(Optional.of(customer));
        when(projectRepository.findDetailedById("project-1")).thenReturn(Optional.of(project));
        when(projectMilestoneRepository.findByIdAndProject_Id(milestone.getId(), "project-1")).thenReturn(Optional.of(milestone));

        var response = projectService.confirmMilestone("project-1", milestone.getId());

        assertThat(response.confirmed()).isTrue();
        assertThat(response.status()).isEqualTo(ProjectMilestoneStatus.CONFIRMED.name());
    }

    @Test
    void milestoneConfirmForbiddenForOtherCustomer() {
        UserAccountEntity customerUser = customerUser();
        authenticateAs(customerUser);
        CustomerProfileEntity currentCustomer = customerProfile("customer-1", customerUser.getId());
        CustomerProfileEntity otherCustomer = customerProfile("customer-2", "user-customer-2");
        ProjectManagementEntity project = project("project-1", otherCustomer, ProjectStatus.ACTIVE.name());
        when(customerProfileRepository.findByUser_Id(customerUser.getId())).thenReturn(Optional.of(currentCustomer));
        when(projectRepository.findDetailedById("project-1")).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.confirmMilestone("project-1", project.getMilestones().get(0).getId()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void closeProjectSuccess() {
        authenticateAs(accountantUser());
        ProjectManagementEntity project = project("project-1", customerProfile("customer-1", "user-customer-1"), ProjectStatus.COMPLETED.name());
        project.getMilestones().forEach(milestone -> {
            milestone.setStatus(ProjectMilestoneStatus.CONFIRMED.name());
            milestone.setConfirmationStatus(ProjectMilestoneStatus.CONFIRMED.name());
            milestone.setConfirmed(Boolean.TRUE);
        });
        project.setPaymentsReceived(new BigDecimal("100.00"));
        project.setPaymentsDue(new BigDecimal("100.00"));
        project.setOutstandingBalance(BigDecimal.ZERO.setScale(2));
        when(projectRepository.findDetailedById("project-1")).thenReturn(Optional.of(project));

        ProjectCloseRequest request = new ProjectCloseRequest();
        request.setCloseReason("Completed");
        request.setCustomerSignoffCompleted(true);

        var response = projectService.closeProject("project-1", request);

        assertThat(response.status()).isEqualTo(ProjectStatus.CLOSED.name());
        assertThat(response.closedAt()).isNotNull();
    }

    @Test
    void closeProjectFailsWhenDebtsRemain() {
        authenticateAs(accountantUser());
        ProjectManagementEntity project = project("project-1", customerProfile("customer-1", "user-customer-1"), ProjectStatus.COMPLETED.name());
        project.getMilestones().forEach(milestone -> {
            milestone.setStatus(ProjectMilestoneStatus.CONFIRMED.name());
            milestone.setConfirmationStatus(ProjectMilestoneStatus.CONFIRMED.name());
            milestone.setConfirmed(Boolean.TRUE);
        });
        project.setOutstandingBalance(new BigDecimal("5.00"));
        when(projectRepository.findDetailedById("project-1")).thenReturn(Optional.of(project));

        ProjectCloseRequest request = new ProjectCloseRequest();
        request.setCloseReason("Completed");
        request.setCustomerSignoffCompleted(true);

        assertThatThrownBy(() -> projectService.closeProject("project-1", request))
                .isInstanceOf(ProjectCloseNotAllowedException.class);
    }

    private void authenticateAs(UserAccountEntity user) {
        when(currentUserProvider.getCurrentUser()).thenReturn(new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole().getName(), "token"));
        when(userAccountRepository.findWithRoleById(user.getId())).thenReturn(Optional.of(user));
    }

    private void mockDetailQueries(ProjectManagementEntity project) {
        when(projectMilestoneRepository.findByProject_IdOrderByCompletionPercentAsc(project.getId())).thenReturn(project.getMilestones());
        when(projectProgressUpdateRepository.findByProject_IdOrderByCreatedAtDesc(project.getId())).thenReturn(List.of());
        when(projectDocumentRepository.findByProject_IdOrderByUploadedAtDesc(project.getId())).thenReturn(List.of());
        when(projectWarehouseAssignmentRepository.findByProject_IdOrderByAssignedAtDesc(project.getId())).thenReturn(List.of());
    }

    private ProjectCreateRequest createRequest() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setCustomerId("customer-1");
        request.setName("Bridge Project");
        request.setLocation("Ho Chi Minh City");
        request.setScope("Foundation and steel frame");
        request.setStartDate(LocalDate.now(APP_ZONE));
        request.setEndDate(LocalDate.now(APP_ZONE).plusMonths(3));
        request.setBudget(new BigDecimal("1000000.00"));
        request.setAssignedProjectManager("Nguyen Van A");
        request.setPrimaryWarehouseId("wh-1");
        request.setPaymentMilestones(List.of(
                milestoneRequest("Milestone 1", 30),
                milestoneRequest("Milestone 2", 60),
                milestoneRequest("Milestone 3", 100)
        ));
        return request;
    }

    private ProjectMilestoneRequest milestoneRequest(String name, int completionPercent) {
        ProjectMilestoneRequest request = new ProjectMilestoneRequest();
        request.setName(name);
        request.setCompletionPercent(completionPercent);
        request.setAmount(new BigDecimal("100000.00"));
        request.setDueDate(LocalDate.now(APP_ZONE).plusDays(completionPercent));
        return request;
    }

    private ProjectDocumentMetadataRequest documentRequest(String fileName) {
        ProjectDocumentMetadataRequest request = new ProjectDocumentMetadataRequest();
        request.setFileName(fileName);
        request.setFileUrl("https://example.com/" + fileName);
        request.setContentType("image/jpeg");
        return request;
    }

    private ProjectManagementEntity project(String id, CustomerProfileEntity customer, String status) {
        ProjectManagementEntity project = new ProjectManagementEntity();
        project.setId(id);
        project.setProjectCode("PRJ-2026-0001");
        project.setCustomer(customer);
        project.setName("Project Name");
        project.setLocation("Project Location");
        project.setBudget(new BigDecimal("100.00"));
        project.setStartDate(LocalDate.now(APP_ZONE).minusDays(10));
        project.setEndDate(LocalDate.now(APP_ZONE).plusDays(20));
        project.setStatus(status);
        project.setAssignedProjectManager("Project Manager");
        project.setProgressPercent(25);
        project.setProgressStatus("ON_TRACK");
        project.setActualSpend(BigDecimal.ZERO.setScale(2));
        project.setCommitments(BigDecimal.ZERO.setScale(2));
        project.setPaymentsReceived(BigDecimal.ZERO.setScale(2));
        project.setPaymentsDue(BigDecimal.ZERO.setScale(2));
        project.setOutstandingBalance(BigDecimal.ZERO.setScale(2));
        project.setOpenOrderCount(0);
        project.setUnresolvedIssueCount(0);
        project.setCreatedAt(LocalDateTime.now(APP_ZONE).minusDays(12));
        project.setUpdatedAt(LocalDateTime.now(APP_ZONE));
        project.setMilestones(new ArrayList<>(List.of(
                milestoneEntity(project, "milestone-1", 30),
                milestoneEntity(project, "milestone-2", 60),
                milestoneEntity(project, "milestone-3", 100)
        )));
        return project;
    }

    private ProjectMilestoneEntity milestoneEntity(ProjectManagementEntity project, String id, int completionPercent) {
        ProjectMilestoneEntity milestone = new ProjectMilestoneEntity();
        milestone.setId(id);
        milestone.setProject(project);
        milestone.setName("Milestone " + completionPercent);
        milestone.setMilestoneType("PAYMENT");
        milestone.setCompletionPercent(completionPercent);
        milestone.setAmount(new BigDecimal("100.00"));
        milestone.setStatus(ProjectMilestoneStatus.PENDING.name());
        milestone.setConfirmationStatus(ProjectMilestoneStatus.PENDING.name());
        milestone.setConfirmed(Boolean.FALSE);
        milestone.setPaymentReleaseReady(Boolean.FALSE);
        return milestone;
    }

    private WarehouseEntity warehouse(String id) {
        WarehouseEntity warehouse = new WarehouseEntity();
        warehouse.setId(id);
        warehouse.setName("Warehouse " + id);
        warehouse.setLocation("HCMC");
        return warehouse;
    }

    private CustomerProfileEntity customerProfile(String id, String userId) {
        CustomerProfileEntity customer = new CustomerProfileEntity();
        customer.setId(id);
        customer.setCompanyName("Customer " + id);
        UserAccountEntity user = new UserAccountEntity();
        user.setId(userId);
        user.setEmail(userId + "@example.com");
        user.setRole(role(RoleName.CUSTOMER));
        customer.setUser(user);
        return customer;
    }

    private UserAccountEntity accountantUser() {
        UserAccountEntity user = new UserAccountEntity();
        user.setId("user-accountant-1");
        user.setEmail("accountant@example.com");
        user.setRole(role(RoleName.ACCOUNTANT));
        return user;
    }

    private UserAccountEntity customerUser() {
        UserAccountEntity user = new UserAccountEntity();
        user.setId("user-customer-1");
        user.setEmail("customer@example.com");
        user.setRole(role(RoleName.CUSTOMER));
        return user;
    }

    private RoleEntity role(RoleName roleName) {
        RoleEntity role = new RoleEntity();
        role.setId("role-" + roleName.name().toLowerCase());
        role.setName(roleName.name());
        return role;
    }
}
