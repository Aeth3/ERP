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

import com.maiu.erp.modules.finance.application.service.ApAgingService;
import com.maiu.erp.modules.finance.application.service.ApBillService;
import com.maiu.erp.modules.finance.application.service.ApPaymentService;
import com.maiu.erp.modules.finance.domain.enums.BillStatus;
import com.maiu.erp.modules.finance.domain.model.Bill;
import com.maiu.erp.modules.finance.domain.model.Payment;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class ApControllerTest {

    private MockMvc mockMvc;
    private ApBillService billService;
    private ApPaymentService paymentService;
    private ApAgingService agingService;

    @BeforeEach
    void setUp() {
        billService = Mockito.mock(ApBillService.class);
        paymentService = Mockito.mock(ApPaymentService.class);
        agingService = Mockito.mock(ApAgingService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ApController(billService, paymentService, agingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createBillReturnsCreatedBill() throws Exception {
        Bill bill = new Bill();
        bill.setId(UUID.randomUUID());
        bill.setStatus(BillStatus.DRAFT);
        when(billService.createDraft(any())).thenReturn(bill);

        mockMvc.perform(post("/finance/ap/bills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": "%s",
                                  "billNumber": "INV-1",
                                  "billDate": "2026-11-05",
                                  "lines": [
                                    { "accountId": "%s", "amount": 100.00, "description": "rent" }
                                  ]
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(bill.getId().toString()));
    }

    @Test
    void approveReturnsBillAndDelegates() throws Exception {
        UUID billId = UUID.randomUUID();
        Bill bill = new Bill();
        bill.setId(billId);
        bill.setStatus(BillStatus.APPROVED);
        when(billService.approve(billId)).thenReturn(bill);

        mockMvc.perform(post("/finance/ap/bills/{id}/approve", billId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(billService).approve(billId);
    }

    @Test
    void approveSurfacesMatchErrorAsBadRequest() throws Exception {
        UUID billId = UUID.randomUUID();
        when(billService.approve(billId))
                .thenThrow(new BadRequestException("Billed quantity would exceed the billable quantity"));

        mockMvc.perform(post("/finance/ap/bills/{id}/approve", billId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listBillsPassesFilters() throws Exception {
        UUID supplierId = UUID.randomUUID();
        when(billService.search(eq(supplierId), eq(BillStatus.APPROVED), eq(null))).thenReturn(List.of());

        mockMvc.perform(get("/finance/ap/bills")
                        .param("supplierId", supplierId.toString())
                        .param("status", "APPROVED"))
                .andExpect(status().isOk());

        verify(billService).search(eq(supplierId), eq(BillStatus.APPROVED), eq(null));
    }

    @Test
    void createPaymentReturnsCreated() throws Exception {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setPaymentNumber("PAY-202611-0001");
        when(paymentService.create(any())).thenReturn(payment);

        mockMvc.perform(post("/finance/ap/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": "%s",
                                  "paymentDate": "2026-11-10",
                                  "method": "BANK",
                                  "allocations": [ { "billId": "%s", "amount": 40.00 } ]
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentNumber").value("PAY-202611-0001"));
    }

    @Test
    void agingReturnsRows() throws Exception {
        UUID supplierId = UUID.randomUUID();
        when(agingService.aging()).thenReturn(List.of(new ApAgingService.AgingRow(
                supplierId, new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("10.00"))));

        mockMvc.perform(get("/finance/ap/aging"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supplierId").value(supplierId.toString()))
                .andExpect(jsonPath("$[0].total").value(10.00));
    }

    @Test
    void billableLinesReturnsRemainingQuantities() throws Exception {
        UUID poId = UUID.randomUUID();
        when(billService.billableLines(poId)).thenReturn(List.of(new ApBillService.BillableLine(
                UUID.randomUUID(), UUID.randomUUID(), "Rebar 10mm", new BigDecimal("10.00"),
                new BigDecimal("3.00"), new BigDecimal("5.00"), new BigDecimal("2.00"), new BigDecimal("20.00"))));

        mockMvc.perform(get("/finance/ap/billable").param("purchaseOrderId", poId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productName").value("Rebar 10mm"))
                .andExpect(jsonPath("$[0].remainingQuantity").value(2.00));
    }

    @Test
    void getBillReturnsIt() throws Exception {
        UUID billId = UUID.randomUUID();
        Bill bill = new Bill();
        bill.setId(billId);
        bill.setBillDate(LocalDate.of(2026, 11, 5));
        bill.setStatus(BillStatus.DRAFT);
        when(billService.get(billId)).thenReturn(bill);

        mockMvc.perform(get("/finance/ap/bills/{id}", billId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(billId.toString()));
    }
}
