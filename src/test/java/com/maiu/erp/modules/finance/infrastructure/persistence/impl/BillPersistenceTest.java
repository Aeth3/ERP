package com.maiu.erp.modules.finance.infrastructure.persistence.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.maiu.erp.modules.finance.domain.enums.BillStatus;
import com.maiu.erp.modules.finance.domain.enums.PaymentMethod;
import com.maiu.erp.modules.finance.domain.model.Bill;
import com.maiu.erp.modules.finance.domain.model.BillLine;
import com.maiu.erp.modules.finance.domain.model.Payment;
import com.maiu.erp.modules.finance.domain.model.PaymentAllocation;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({ BillRepositoryImpl.class, PaymentRepositoryImpl.class })
class BillPersistenceTest {

    @Autowired
    private BillRepositoryImpl billRepository;

    @Autowired
    private PaymentRepositoryImpl paymentRepository;

    @Test
    void billAndPaymentRoundTripWithOrderedLinesAndAllocations() {
        UUID supplierId = UUID.randomUUID();
        UUID poId = UUID.randomUUID();

        Bill bill = new Bill();
        bill.setBillNumber("INV-001");
        bill.setSupplierId(supplierId);
        bill.setPurchaseOrderId(poId);
        bill.setBillDate(LocalDate.of(2026, 11, 5));
        bill.setDueDate(LocalDate.of(2026, 12, 5));
        bill.setStatus(BillStatus.DRAFT);
        bill.setSubtotal(new BigDecimal("350.00"));
        bill.setTotal(new BigDecimal("350.00"));
        bill.setAmountPaid(BigDecimal.ZERO);
        bill.setNotes("first delivery");
        bill.setCreatedBy("tester");
        bill.setCreatedAt(Instant.now());

        BillLine first = new BillLine();
        first.setLineNumber(1);
        first.setProductId(UUID.randomUUID());
        first.setPurchaseOrderItemId(UUID.randomUUID());
        first.setQuantity(new BigDecimal("10.00"));
        first.setUnitCost(new BigDecimal("20.00"));
        first.setLineTotal(new BigDecimal("200.00"));

        BillLine second = new BillLine();
        second.setLineNumber(2);
        second.setProductId(UUID.randomUUID());
        second.setPurchaseOrderItemId(UUID.randomUUID());
        second.setQuantity(new BigDecimal("5.00"));
        second.setUnitCost(new BigDecimal("30.00"));
        second.setLineTotal(new BigDecimal("150.00"));
        bill.setLines(List.of(first, second));

        Bill savedBill = billRepository.save(bill);
        Bill reloaded = billRepository.findById(savedBill.getId()).orElseThrow();

        assertEquals("INV-001", reloaded.getBillNumber());
        assertEquals(supplierId, reloaded.getSupplierId());
        assertEquals(poId, reloaded.getPurchaseOrderId());
        assertEquals(BillStatus.DRAFT, reloaded.getStatus());
        assertEquals(2, reloaded.getLines().size());
        assertEquals(1, reloaded.getLines().get(0).getLineNumber());
        assertEquals(new BigDecimal("20.00"), reloaded.getLines().get(0).getUnitCost());
        assertEquals(new BigDecimal("150.00"), reloaded.getLines().get(1).getLineTotal());
        assertEquals(first.getPurchaseOrderItemId(), reloaded.getLines().get(0).getPurchaseOrderItemId());

        assertEquals(1, billRepository.findByPurchaseOrderId(poId).size());
        assertEquals(1, billRepository.search(supplierId, BillStatus.DRAFT, poId).size());
        assertEquals(0, billRepository.search(supplierId, BillStatus.PAID, null).size());
        assertTrue(billRepository.existsBySupplierIdAndBillNumberIgnoreCaseAndStatusNot(
                supplierId, "inv-001", BillStatus.CANCELLED));
        assertFalse(billRepository.existsBySupplierIdAndBillNumberIgnoreCaseAndStatusNot(
                UUID.randomUUID(), "INV-001", BillStatus.CANCELLED));
        assertTrue(billRepository.findByIdForUpdate(savedBill.getId()).isPresent());
        assertEquals(0, billRepository.findOpen().size());

        Payment payment = new Payment();
        payment.setPaymentNumber("PAY-202611-0001");
        payment.setSupplierId(supplierId);
        payment.setPaymentDate(LocalDate.of(2026, 11, 10));
        payment.setMethod(PaymentMethod.BANK);
        payment.setReference("wire 123");
        payment.setTotal(new BigDecimal("350.00"));
        payment.setCreatedBy("tester");
        payment.setCreatedAt(Instant.now());

        PaymentAllocation allocation = new PaymentAllocation();
        allocation.setBillId(savedBill.getId());
        allocation.setAmount(new BigDecimal("350.00"));
        payment.setAllocations(List.of(allocation));

        Payment savedPayment = paymentRepository.save(payment);
        Payment reloadedPayment = paymentRepository.findById(savedPayment.getId()).orElseThrow();

        assertEquals("PAY-202611-0001", reloadedPayment.getPaymentNumber());
        assertEquals(PaymentMethod.BANK, reloadedPayment.getMethod());
        assertEquals(1, reloadedPayment.getAllocations().size());
        assertEquals(savedBill.getId(), reloadedPayment.getAllocations().get(0).getBillId());
        assertEquals(new BigDecimal("350.00"), reloadedPayment.getAllocations().get(0).getAmount());
        assertEquals(1, paymentRepository.findBySupplierId(supplierId).size());
        assertEquals(1, paymentRepository.findAll().size());
    }
}
