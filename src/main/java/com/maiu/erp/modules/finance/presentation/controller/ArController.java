package com.maiu.erp.modules.finance.presentation.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.finance.application.service.ArAgingService;
import com.maiu.erp.modules.finance.application.service.ArInvoiceService;
import com.maiu.erp.modules.finance.application.service.ArReceiptService;
import com.maiu.erp.modules.finance.domain.enums.InvoiceKind;
import com.maiu.erp.modules.finance.domain.enums.InvoiceStatus;
import com.maiu.erp.modules.finance.domain.model.Invoice;
import com.maiu.erp.modules.finance.domain.model.Receipt;

@RestController
@RequestMapping("/finance/ar")
public class ArController {
    private final ArInvoiceService invoiceService;
    private final ArReceiptService receiptService;
    private final ArAgingService agingService;

    public ArController(
            ArInvoiceService invoiceService, ArReceiptService receiptService, ArAgingService agingService) {
        this.invoiceService = invoiceService;
        this.receiptService = receiptService;
        this.agingService = agingService;
    }

    public record ReleaseRetentionRequest(LocalDate invoiceDate, LocalDate dueDate) {
    }

    @PostMapping("/invoices")
    public ResponseEntity<Invoice> createInvoice(@RequestBody ArInvoiceService.CreateInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createDraft(request));
    }

    @PutMapping("/invoices/{id}")
    public ResponseEntity<Invoice> updateInvoice(
            @PathVariable UUID id, @RequestBody ArInvoiceService.CreateInvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.updateDraft(id, request));
    }

    @PostMapping("/invoices/{id}/approve")
    public ResponseEntity<Invoice> approveInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.approve(id));
    }

    @PostMapping("/invoices/{id}/cancel")
    public ResponseEntity<Invoice> cancelInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.cancel(id));
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<Invoice>> listInvoices(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) InvoiceKind kind) {
        return ResponseEntity.ok(invoiceService.search(customerId, status, kind));
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<Invoice> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.get(id));
    }

    @PostMapping("/projects/{projectId}/release-retention")
    public ResponseEntity<Invoice> releaseRetention(
            @PathVariable UUID projectId, @RequestBody ReleaseRetentionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.releaseRetention(projectId, request.invoiceDate(), request.dueDate()));
    }

    @GetMapping("/projects/{projectId}/billing-summary")
    public ResponseEntity<ArInvoiceService.ProjectBillingSummary> billingSummary(@PathVariable UUID projectId) {
        return ResponseEntity.ok(invoiceService.projectBillingSummary(projectId));
    }

    @GetMapping("/invoiceable")
    public ResponseEntity<List<ArInvoiceService.InvoiceableLine>> invoiceableLines(
            @RequestParam UUID salesOrderId) {
        return ResponseEntity.ok(invoiceService.invoiceableLines(salesOrderId));
    }

    @PostMapping("/receipts")
    public ResponseEntity<Receipt> createReceipt(@RequestBody ArReceiptService.CreateReceiptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receiptService.create(request));
    }

    @GetMapping("/receipts")
    public ResponseEntity<List<Receipt>> listReceipts(@RequestParam(required = false) UUID customerId) {
        return ResponseEntity.ok(receiptService.search(customerId));
    }

    @GetMapping("/receipts/{id}")
    public ResponseEntity<Receipt> getReceipt(@PathVariable UUID id) {
        return ResponseEntity.ok(receiptService.get(id));
    }

    @GetMapping("/aging")
    public ResponseEntity<List<ArAgingService.AgingRow>> aging() {
        return ResponseEntity.ok(agingService.aging());
    }
}
