package com.maiu.erp.modules.reporting.presentation.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.reporting.application.dto.ReportingOverviewDto;
import com.maiu.erp.modules.reporting.application.dto.PurchaseOrderReportSummaryDto;
import com.maiu.erp.modules.reporting.application.dto.SalesOrderReportSummaryDto;
import com.maiu.erp.modules.reporting.application.service.ReportingQueryService;

@RestController
@RequestMapping("/reports")
public class ReportingController {
    private final ReportingQueryService reportingQueryService;

    public ReportingController(ReportingQueryService reportingQueryService) {
        this.reportingQueryService = reportingQueryService;
    }

    @GetMapping("/overview")
    public ResponseEntity<ReportingOverviewDto> getOverview(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(reportingQueryService.getOverview(startDate, endDate));
    }

    @GetMapping("/purchase-orders")
    public ResponseEntity<PurchaseOrderReportSummaryDto> getPurchaseOrderReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID supplierId) {
        return ResponseEntity.ok(reportingQueryService.getPurchaseOrderReport(
                startDate,
                endDate,
                status,
                projectId,
                supplierId));
    }

    @GetMapping("/sales-orders")
    public ResponseEntity<SalesOrderReportSummaryDto> getSalesOrderReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID warehouseId) {
        return ResponseEntity.ok(reportingQueryService.getSalesOrderReport(
                startDate,
                endDate,
                status,
                customerId,
                warehouseId));
    }
}
