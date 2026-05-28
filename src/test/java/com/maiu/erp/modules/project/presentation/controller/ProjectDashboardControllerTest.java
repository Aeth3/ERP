package com.maiu.erp.modules.project.presentation.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.maiu.erp.modules.project.application.dto.ProjectDashboardDto;
import com.maiu.erp.modules.project.application.service.ProjectCostQueryService;
import com.maiu.erp.modules.project.application.service.ProjectDashboardQueryService;
import com.maiu.erp.modules.project.application.service.ProjectMovementQueryService;
import com.maiu.erp.modules.project.domain.enums.ProjectStatus;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class ProjectDashboardControllerTest {
    private MockMvc mockMvc;
    private ProjectCostQueryService projectCostQueryService;
    private ProjectMovementQueryService projectMovementQueryService;
    private ProjectDashboardQueryService projectDashboardQueryService;

    @BeforeEach
    void setUp() {
        projectCostQueryService = Mockito.mock(ProjectCostQueryService.class);
        projectMovementQueryService = Mockito.mock(ProjectMovementQueryService.class);
        projectDashboardQueryService = Mockito.mock(ProjectDashboardQueryService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProjectCostController(
                        projectCostQueryService,
                        projectMovementQueryService,
                        projectDashboardQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getProjectDashboardReturnsDashboardPayload() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectDashboardQueryService.getProjectDashboard(projectId))
                .thenReturn(new ProjectDashboardDto(
                        projectId,
                        "Tower A",
                        ProjectStatus.ACTIVE,
                        new BigDecimal("800.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("70.00"),
                        new BigDecimal("500.00"),
                        new BigDecimal("730.00"),
                        new BigDecimal("8.75"),
                        new BigDecimal("5.00"),
                        new BigDecimal("1.50"),
                        new BigDecimal("3.50"),
                        1,
                        1,
                        1,
                        2,
                        Instant.parse("2026-05-26T08:00:00Z")));

        mockMvc.perform(get("/projects/{id}/dashboard", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.budgetUtilizationPercent").value(8.75))
                .andExpect(jsonPath("$.totalReversalCount").value(1))
                .andExpect(jsonPath("$.movementCount").value(2));

        verify(projectDashboardQueryService).getProjectDashboard(eq(projectId));
    }
}
