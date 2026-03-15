package com.g90.backend.modules.project.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.modules.project.dto.ProjectCreateRequest;
import com.g90.backend.modules.project.dto.ProjectDetailResponseData;
import com.g90.backend.modules.project.dto.ProjectFinancialSummaryResponseData;
import com.g90.backend.modules.project.dto.ProjectMilestoneRequest;
import com.g90.backend.modules.project.dto.ProjectResponse;
import com.g90.backend.modules.project.service.ProjectService;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.security.AccessTokenService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void accountantCanCreateProject() throws Exception {
        when(projectService.createProject(any())).thenReturn(projectResponse());

        mockMvc.perform(post("/api/projects")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void canViewProjectDetail() throws Exception {
        when(projectService.getProjectDetail(anyString())).thenReturn(detailResponse());

        mockMvc.perform(get("/api/projects/project-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.project.id").value("project-1"));
    }

    @Test
    void canViewFinancialSummary() throws Exception {
        when(projectService.getFinancialSummary(anyString())).thenReturn(
                new ProjectFinancialSummaryResponseData(
                        new BigDecimal("100.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("100.00"),
                        List.of(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "PROJECT_FIELDS"
                )
        );

        mockMvc.perform(get("/api/projects/project-1/financial-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    private ProjectCreateRequest createRequest() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setCustomerId("customer-1");
        request.setName("Bridge Project");
        request.setLocation("HCMC");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(30));
        request.setBudget(new BigDecimal("1000.00"));
        request.setAssignedProjectManager("Manager");
        request.setPaymentMilestones(List.of(
                milestoneRequest("M1", 30),
                milestoneRequest("M2", 60),
                milestoneRequest("M3", 100)
        ));
        return request;
    }

    private ProjectMilestoneRequest milestoneRequest(String name, int percent) {
        ProjectMilestoneRequest request = new ProjectMilestoneRequest();
        request.setName(name);
        request.setCompletionPercent(percent);
        return request;
    }

    private ProjectResponse projectResponse() {
        return new ProjectResponse(
                "project-1",
                "PRJ-2026-0001",
                "customer-1",
                "Customer",
                "Bridge Project",
                "HCMC",
                "Scope",
                "ACTIVE",
                0,
                "ON_TRACK",
                null,
                new BigDecimal("1000.00"),
                "NOT_REQUIRED",
                "NOT_REQUIRED",
                "Manager",
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private ProjectDetailResponseData detailResponse() {
        return new ProjectDetailResponseData(
                projectResponse(),
                new ProjectDetailResponseData.Timeline(LocalDate.now(), LocalDate.now().plusDays(30), null, false, false),
                new ProjectFinancialSummaryResponseData(
                        new BigDecimal("1000.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("1000.00"),
                        List.of(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "PROJECT_FIELDS"
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new ProjectDetailResponseData.PaymentStatus("NOT_STARTED", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(),
                new ProjectDetailResponseData.Warehouses(null, null, List.of())
        );
    }
}
