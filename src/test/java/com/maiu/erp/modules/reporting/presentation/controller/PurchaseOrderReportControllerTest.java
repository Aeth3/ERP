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

import com.maiu.erp.modules.reporting.application.dto.PurchaseOrderReportItemDto;
import com.maiu.erp.modules.reporting.application.dto.PurchaseOrderReportSummaryDto;
import com.maiu.erp.modules.reporting.application.service.ReportingQueryService;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class PurchaseOrderReportControllerTest {
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
    void getPurchaseOrderReportReturnsFilteredPayload() throws Exception {
        UUID supplierId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        when(reportingQueryService.getPurchaseOrderReport(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "APPROVED",
                projectId,
                supplierId))
                .thenReturn(new PurchaseOrderReportSummaryDto(
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30),
                        1,
                        new BigDecimal("250.00"),
                        List.of(new PurchaseOrderReportItemDto(
                                UUID.randomUUID(),
                                "PO-123",
                                "APPROVED",
                                LocalDate.of(2026, 6, 10),
                                LocalDate.of(2026, 6, 20),
                                new BigDecimal("250.00"),
                                Instant.parse("2026-06-10T08:00:00Z"),
                                supplierId,
                                "SUP-001",
                                "Build Supply",
                                projectId,
                                "PRJ-001",
                                "Fitout",
                                null,
                                null,
                                null,
                                2,
                                0))));

        mockMvc.perform(get("/reports/purchase-orders")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-30")
                        .param("status", "APPROVED")
                        .param("projectId", projectId.toString())
                        .param("supplierId", supplierId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(1))
                .andExpect(jsonPath("$.totalAmount").value(250.00))
                .andExpect(jsonPath("$.orders[0].supplierName").value("Build Supply"))
                .andExpect(jsonPath("$.orders[0].projectCode").value("PRJ-001"));

        verify(reportingQueryService).getPurchaseOrderReport(
                eq(LocalDate.of(2026, 6, 1)),
                eq(LocalDate.of(2026, 6, 30)),
                eq("APPROVED"),
                eq(projectId),
                eq(supplierId));
    }
}
