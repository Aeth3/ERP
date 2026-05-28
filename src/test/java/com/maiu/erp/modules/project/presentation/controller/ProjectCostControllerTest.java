package com.maiu.erp.modules.project.presentation.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.maiu.erp.modules.inventory.domain.enums.MovementType;
import com.maiu.erp.modules.project.application.dto.ProjectCostSummaryDto;
import com.maiu.erp.modules.project.application.dto.ProjectMovementDto;
import com.maiu.erp.modules.project.application.service.ProjectCostQueryService;
import com.maiu.erp.modules.project.application.service.ProjectDashboardQueryService;
import com.maiu.erp.modules.project.application.service.ProjectMovementQueryService;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class ProjectCostControllerTest {

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
    void getProjectMovementsReturnsReversalFlag() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();

        when(projectMovementQueryService.getProjectMovements(projectId))
                .thenReturn(List.of(new ProjectMovementDto(
                        movementId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        MovementType.RETURN,
                        new BigDecimal("2.00"),
                        new BigDecimal("12.50"),
                        "MATERIAL_RETURN",
                        UUID.randomUUID(),
                        true,
                        "Reversal of MI-0003",
                        "warehouse lead",
                        Instant.parse("2026-05-26T07:00:00Z"))));

        mockMvc.perform(get("/projects/{id}/movements", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(movementId.toString()))
                .andExpect(jsonPath("$[0].movementType").value("RETURN"))
                .andExpect(jsonPath("$[0].referenceType").value("MATERIAL_RETURN"))
                .andExpect(jsonPath("$[0].reversal").value(true));

        verify(projectMovementQueryService).getProjectMovements(eq(projectId));
    }

    @Test
    void getProjectCostSummaryReturnsSummaryPayload() throws Exception {
        UUID projectId = UUID.randomUUID();

        when(projectCostQueryService.getProjectCostSummary(projectId))
                .thenReturn(new ProjectCostSummaryDto(
                        projectId,
                        "Tower A",
                        new BigDecimal("1000.00"),
                        new BigDecimal("1400.00"),
                        new BigDecimal("1400.00"),
                        new BigDecimal("250.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("60.00"),
                        new BigDecimal("240.00"),
                        new BigDecimal("250.00"),
                        new BigDecimal("240.00"),
                        new BigDecimal("1150.00"),
                        new BigDecimal("1160.00"),
                        new BigDecimal("760.00"),
                        List.of()));

        mockMvc.perform(get("/projects/{id}/cost-summary", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.budgetBasisAmount").value(1400.00))
                .andExpect(jsonPath("$.committedCost").value(250.00))
                .andExpect(jsonPath("$.actualCost").value(240.00))
                .andExpect(jsonPath("$.netMaterialIssuedCost").value(240.00));
    }
}
