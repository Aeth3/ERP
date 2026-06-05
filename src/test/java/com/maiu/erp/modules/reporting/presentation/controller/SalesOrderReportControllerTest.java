package com.maiu.erp.modules.reporting.presentation.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.maiu.erp.modules.reporting.application.dto.SalesOrderReportItemDto;
import com.maiu.erp.modules.reporting.application.dto.SalesOrderReportSummaryDto;
import com.maiu.erp.modules.reporting.application.service.ReportingQueryService;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class SalesOrderReportControllerTest {
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
    void getSalesOrderReportReturnsFilteredPayload() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        when(reportingQueryService.getSalesOrderReport(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "CONFIRMED",
                customerId,
                warehouseId))
                .thenReturn(new SalesOrderReportSummaryDto(
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30),
                        1,
                        new BigDecimal("180.00"),
                        List.of(new SalesOrderReportItemDto(
                                UUID.randomUUID(),
                                "SO-123",
                                "CONFIRMED",
                                LocalDate.of(2026, 6, 12),
                                new BigDecimal("180.00"),
                                customerId,
                                "CUS-001",
                                "Acme Client",
                                warehouseId,
                                "WH-001",
                                "Main Warehouse",
                                2,
                                0))));

        mockMvc.perform(get("/reports/sales-orders")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-30")
                        .param("status", "CONFIRMED")
                        .param("customerId", customerId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(1))
                .andExpect(jsonPath("$.totalAmount").value(180.00))
                .andExpect(jsonPath("$.orders[0].customerName").value("Acme Client"))
                .andExpect(jsonPath("$.orders[0].confirmedWarehouseCode").value("WH-001"));

        verify(reportingQueryService).getSalesOrderReport(
                eq(LocalDate.of(2026, 6, 1)),
                eq(LocalDate.of(2026, 6, 30)),
                eq("CONFIRMED"),
                eq(customerId),
                eq(warehouseId));
    }
}
