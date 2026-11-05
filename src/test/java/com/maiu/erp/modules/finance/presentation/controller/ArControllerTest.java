package com.maiu.erp.modules.finance.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.maiu.erp.modules.finance.application.service.ArAgingService;
import com.maiu.erp.modules.finance.application.service.ArInvoiceService;
import com.maiu.erp.modules.finance.application.service.ArReceiptService;
import com.maiu.erp.modules.finance.domain.enums.InvoiceKind;
import com.maiu.erp.modules.finance.domain.enums.InvoiceStatus;
import com.maiu.erp.modules.finance.domain.model.Invoice;
import com.maiu.erp.modules.finance.domain.model.Receipt;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class ArControllerTest {

    private MockMvc mockMvc;
    private ArInvoiceService invoiceService;
    private ArReceiptService receiptService;
    private ArAgingService agingService;

    @BeforeEach
    void setUp() {
        invoiceService = Mockito.mock(ArInvoiceService.class);
        receiptService = Mockito.mock(ArReceiptService.class);
        agingService = Mockito.mock(ArAgingService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ArController(invoiceService, receiptService, agingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createInvoiceReturnsCreated() throws Exception {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.DRAFT);
        when(invoiceService.createDraft(any())).thenReturn(invoice);

        mockMvc.perform(post("/finance/ar/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "SALES_ORDER",
                                  "customerId": "%s",
                                  "salesOrderId": "%s",
                                  "invoiceDate": "2026-11-05",
                                  "lines": [ { "salesOrderItemId": "%s", "quantity": 2.00 } ]
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(invoice.getId().toString()));
    }

    @Test
    void approveSurfacesMatchErrorAsBadRequest() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.approve(invoiceId))
                .thenThrow(new BadRequestException("Invoiced quantity would exceed the shipped quantity"));

        mockMvc.perform(post("/finance/ar/invoices/{id}/approve", invoiceId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listInvoicesPassesFilters() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(invoiceService.search(eq(customerId), eq(InvoiceStatus.APPROVED), eq(InvoiceKind.PROGRESS_BILLING)))
                .thenReturn(List.of());

        mockMvc.perform(get("/finance/ar/invoices")
                        .param("customerId", customerId.toString())
                        .param("status", "APPROVED")
                        .param("kind", "PROGRESS_BILLING"))
                .andExpect(status().isOk());

        verify(invoiceService).search(eq(customerId), eq(InvoiceStatus.APPROVED), eq(InvoiceKind.PROGRESS_BILLING));
    }

    @Test
    void releaseRetentionReturnsCreatedDraft() throws Exception {
        UUID projectId = UUID.randomUUID();
        Invoice release = new Invoice();
        release.setId(UUID.randomUUID());
        release.setKind(InvoiceKind.RETENTION_RELEASE);
        release.setStatus(InvoiceStatus.DRAFT);
        when(invoiceService.releaseRetention(eq(projectId), eq(LocalDate.of(2026, 11, 5)), eq(null)))
                .thenReturn(release);

        mockMvc.perform(post("/finance/ar/projects/{projectId}/release-retention", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"invoiceDate\": \"2026-11-05\" }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("RETENTION_RELEASE"));
    }

    @Test
    void billingSummaryReturnsAggregates() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(invoiceService.projectBillingSummary(projectId)).thenReturn(new ArInvoiceService.ProjectBillingSummary(
                projectId, "PRJ-7", new BigDecimal("10000.00"), new BigDecimal("10.00"),
                new BigDecimal("1000.00"), new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("9000.00")));

        mockMvc.perform(get("/finance/ar/projects/{projectId}/billing-summary", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectCode").value("PRJ-7"))
                .andExpect(jsonPath("$.remainingContract").value(9000.00));
    }

    @Test
    void createReceiptReturnsCreated() throws Exception {
        Receipt receipt = new Receipt();
        receipt.setId(UUID.randomUUID());
        receipt.setReceiptNumber("RCT-202611-0001");
        when(receiptService.create(any())).thenReturn(receipt);

        mockMvc.perform(post("/finance/ar/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "receiptDate": "2026-11-10",
                                  "method": "BANK",
                                  "allocations": [ { "invoiceId": "%s", "amount": 40.00 } ]
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptNumber").value("RCT-202611-0001"));
    }

    @Test
    void agingReturnsRows() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(agingService.aging()).thenReturn(List.of(new ArAgingService.AgingRow(
                customerId, new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("10.00"))));

        mockMvc.perform(get("/finance/ar/aging"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()))
                .andExpect(jsonPath("$[0].total").value(10.00));
    }

    @Test
    void invoiceableReturnsRemaining() throws Exception {
        UUID soId = UUID.randomUUID();
        when(invoiceService.invoiceableLines(soId)).thenReturn(List.of(new ArInvoiceService.InvoiceableLine(
                UUID.randomUUID(), UUID.randomUUID(), "Rebar 10mm", new BigDecimal("10.00"),
                new BigDecimal("2.00"), new BigDecimal("3.00"), new BigDecimal("5.00"), new BigDecimal("25.00"))));

        mockMvc.perform(get("/finance/ar/invoiceable").param("salesOrderId", soId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productName").value("Rebar 10mm"))
                .andExpect(jsonPath("$[0].remainingQuantity").value(5.00));
    }
}
