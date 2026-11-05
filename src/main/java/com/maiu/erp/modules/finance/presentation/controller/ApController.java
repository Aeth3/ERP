package com.maiu.erp.modules.finance.presentation.controller;

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

import com.maiu.erp.modules.finance.application.service.ApAgingService;
import com.maiu.erp.modules.finance.application.service.ApBillService;
import com.maiu.erp.modules.finance.application.service.ApPaymentService;
import com.maiu.erp.modules.finance.domain.enums.BillStatus;
import com.maiu.erp.modules.finance.domain.model.Bill;
import com.maiu.erp.modules.finance.domain.model.Payment;

@RestController
@RequestMapping("/finance/ap")
public class ApController {
    private final ApBillService billService;
    private final ApPaymentService paymentService;
    private final ApAgingService agingService;

    public ApController(ApBillService billService, ApPaymentService paymentService, ApAgingService agingService) {
        this.billService = billService;
        this.paymentService = paymentService;
        this.agingService = agingService;
    }

    @PostMapping("/bills")
    public ResponseEntity<Bill> createBill(@RequestBody ApBillService.CreateBillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billService.createDraft(request));
    }

    @PutMapping("/bills/{id}")
    public ResponseEntity<Bill> updateBill(
            @PathVariable UUID id, @RequestBody ApBillService.CreateBillRequest request) {
        return ResponseEntity.ok(billService.updateDraft(id, request));
    }

    @PostMapping("/bills/{id}/approve")
    public ResponseEntity<Bill> approveBill(@PathVariable UUID id) {
        return ResponseEntity.ok(billService.approve(id));
    }

    @PostMapping("/bills/{id}/cancel")
    public ResponseEntity<Bill> cancelBill(@PathVariable UUID id) {
        return ResponseEntity.ok(billService.cancel(id));
    }

    @GetMapping("/bills")
    public ResponseEntity<List<Bill>> listBills(
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) BillStatus status,
            @RequestParam(required = false) UUID purchaseOrderId) {
        return ResponseEntity.ok(billService.search(supplierId, status, purchaseOrderId));
    }

    @GetMapping("/bills/{id}")
    public ResponseEntity<Bill> getBill(@PathVariable UUID id) {
        return ResponseEntity.ok(billService.get(id));
    }

    @PostMapping("/payments")
    public ResponseEntity<Payment> createPayment(@RequestBody ApPaymentService.CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(request));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<Payment>> listPayments(@RequestParam(required = false) UUID supplierId) {
        return ResponseEntity.ok(paymentService.search(supplierId));
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.get(id));
    }

    @GetMapping("/billable")
    public ResponseEntity<List<ApBillService.BillableLine>> billableLines(@RequestParam UUID purchaseOrderId) {
        return ResponseEntity.ok(billService.billableLines(purchaseOrderId));
    }

    @GetMapping("/aging")
    public ResponseEntity<List<ApAgingService.AgingRow>> aging() {
        return ResponseEntity.ok(agingService.aging());
    }
}
