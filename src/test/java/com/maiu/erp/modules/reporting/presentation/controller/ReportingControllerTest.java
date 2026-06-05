package com.maiu.erp.modules.reporting.presentation.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.maiu.erp.modules.project.domain.enums.ProjectStatus;
import com.maiu.erp.modules.reporting.application.dto.InventorySummaryDto;
import com.maiu.erp.modules.reporting.application.dto.MovementSummaryDto;
import com.maiu.erp.modules.reporting.application.dto.MovementTypeCountDto;
import com.maiu.erp.modules.reporting.application.dto.ProjectRiskItemDto;
import com.maiu.erp.modules.reporting.application.dto.ProjectSummaryDto;
import com.maiu.erp.modules.reporting.application.dto.ReportMovementItemDto;
import com.maiu.erp.modules.reporting.application.dto.ReportingOverviewDto;
import com.maiu.erp.modules.reporting.application.service.ReportingQueryService;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class ReportingControllerTest {
    private MockMvc mockMvc;
    private ReportingQueryService reportingQueryService;

    @BeforeEach
    void setUp() {
        reportingQueryService = Mockito.mock(ReportingQueryService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReportingController(reportingQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getOverviewReturnsAggregatePayload() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();

        when(reportingQueryService.getOverview(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)))
                .thenReturn(new ReportingOverviewDto(
                        new InventorySummaryDto(
                                new BigDecimal("10.00"),
                                new BigDecimal("2.00"),
                                new BigDecimal("8.00"),
                                1,
                                4,
                                2,
                                1),
                        new MovementSummaryDto(
                                LocalDate.of(2026, 6, 1),
                                LocalDate.of(2026, 6, 30),
                                1,
                                List.of(new MovementTypeCountDto("PROJECT_ISSUE", 1)),
                                List.of(new ReportMovementItemDto(
                                        UUID.randomUUID(),
                                        "PROJECT_ISSUE",
                                        new BigDecimal("3.00"),
                                        new BigDecimal("20.00"),
                                        "MATERIAL_ISSUE",
                                        UUID.randomUUID(),
                                        "Issued to site",
                                        "warehouse lead",
                                        Instant.parse("2026-06-01T08:00:00Z"),
                                        productId,
                                        "Cement",
                                        "CEM-001",
                                        warehouseId,
                                        "Main Warehouse",
                                        "WH-001",
                                        projectId,
                                        "PRJ-001",
                                        "Cebu Fitout"))),
                        new ProjectSummaryDto(
                                3,
                                1,
                                1,
                                new BigDecimal("120.00"),
                                new BigDecimal("40.00"),
                                List.of(new ProjectRiskItemDto(
                                        projectId,
                                        "PRJ-001",
                                        "Cebu Fitout",
                                        ProjectStatus.ACTIVE,
                                        new BigDecimal("100.00"),
                                        new BigDecimal("120.00"),
                                        new BigDecimal("40.00"),
                                        new BigDecimal("-20.00"))))));

        mockMvc.perform(get("/reports/overview")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventory.totalAvailable").value(8.00))
                .andExpect(jsonPath("$.movement.totalMovementCount").value(1))
                .andExpect(jsonPath("$.movement.recentMovements[0].productName").value("Cement"))
                .andExpect(jsonPath("$.project.activeProjectCount").value(1))
                .andExpect(jsonPath("$.project.budgetRiskProjects[0].projectCode").value("PRJ-001"));

        verify(reportingQueryService).getOverview(eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 30)));
    }
}
