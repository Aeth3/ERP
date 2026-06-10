package com.maiu.erp.modules.reporting.presentation.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.reporting.application.dto.PurchaseOrderReportDetailDto;
import com.maiu.erp.modules.reporting.application.dto.PurchaseOrderReportSummaryDto;
import com.maiu.erp.modules.reporting.application.dto.ReportingOverviewDto;
import com.maiu.erp.modules.reporting.application.dto.SalesOrderReportDetailDto;
import com.maiu.erp.modules.reporting.application.dto.SalesOrderReportSummaryDto;
import com.maiu.erp.modules.reporting.application.dto.StockMovementReportDetailDto;
import com.maiu.erp.modules.reporting.application.dto.StockMovementReportSummaryDto;
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
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportingQueryService.getPurchaseOrderReport(
                startDate,
                endDate,
                status,
                projectId,
                supplierId,
                page,
                size));
    }

    @GetMapping("/purchase-orders/{purchaseOrderId}")
    public ResponseEntity<PurchaseOrderReportDetailDto> getPurchaseOrderReportDetail(
            @PathVariable UUID purchaseOrderId) {
        return ResponseEntity.ok(reportingQueryService.getPurchaseOrderReportDetail(purchaseOrderId));
    }

    @GetMapping("/purchase-orders/export")
    public ResponseEntity<byte[]> exportPurchaseOrders(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID supplierId) {
        return csvResponse(
                "purchase-orders.csv",
                reportingQueryService.exportPurchaseOrderReportCsv(startDate, endDate, status, projectId, supplierId));
    }

    @GetMapping("/sales-orders")
    public ResponseEntity<SalesOrderReportSummaryDto> getSalesOrderReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportingQueryService.getSalesOrderReport(
                startDate,
                endDate,
                status,
                customerId,
                warehouseId,
                page,
                size));
    }

    @GetMapping("/sales-orders/{salesOrderId}")
    public ResponseEntity<SalesOrderReportDetailDto> getSalesOrderReportDetail(
            @PathVariable UUID salesOrderId) {
        return ResponseEntity.ok(reportingQueryService.getSalesOrderReportDetail(salesOrderId));
    }

    @GetMapping("/sales-orders/export")
    public ResponseEntity<byte[]> exportSalesOrders(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID warehouseId) {
        return csvResponse(
                "sales-orders.csv",
                reportingQueryService.exportSalesOrderReportCsv(startDate, endDate, status, customerId, warehouseId));
    }

    @GetMapping("/stock-movements")
    public ResponseEntity<StockMovementReportSummaryDto> getStockMovementReport(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) String movementType,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportingQueryService.getStockMovementReport(
                productId,
                warehouseId,
                projectId,
                referenceType,
                movementType,
                startDate,
                endDate,
                page,
                size));
    }

    @GetMapping("/stock-movements/{stockMovementId}")
    public ResponseEntity<StockMovementReportDetailDto> getStockMovementReportDetail(
            @PathVariable UUID stockMovementId) {
        return ResponseEntity.ok(reportingQueryService.getStockMovementReportDetail(stockMovementId));
    }

    @GetMapping("/stock-movements/export")
    public ResponseEntity<byte[]> exportStockMovements(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) String movementType,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return csvResponse(
                "stock-movements.csv",
                reportingQueryService.exportStockMovementReportCsv(
                        productId,
                        warehouseId,
                        projectId,
                        referenceType,
                        movementType,
                        startDate,
                        endDate));
    }

    private ResponseEntity<byte[]> csvResponse(String filename, String content) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(content.getBytes(StandardCharsets.UTF_8));
    }
}
