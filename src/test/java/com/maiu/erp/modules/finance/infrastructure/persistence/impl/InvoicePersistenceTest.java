package com.maiu.erp.modules.finance.infrastructure.persistence.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.maiu.erp.modules.finance.domain.enums.InvoiceKind;
import com.maiu.erp.modules.finance.domain.enums.InvoiceStatus;
import com.maiu.erp.modules.finance.domain.enums.PaymentMethod;
import com.maiu.erp.modules.finance.domain.model.Invoice;
import com.maiu.erp.modules.finance.domain.model.InvoiceLine;
import com.maiu.erp.modules.finance.domain.model.Receipt;
import com.maiu.erp.modules.finance.domain.model.ReceiptAllocation;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({ InvoiceRepositoryImpl.class, ReceiptRepositoryImpl.class })
class InvoicePersistenceTest {

    @Autowired
    private InvoiceRepositoryImpl invoiceRepository;

    @Autowired
    private ReceiptRepositoryImpl receiptRepository;

    private Invoice invoice(UUID customerId, InvoiceKind kind, InvoiceStatus status, String number) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(number);
        invoice.setKind(kind);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceDate(LocalDate.of(2026, 11, 5));
        invoice.setStatus(status);
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setRetentionAmount(BigDecimal.ZERO);
        invoice.setTotal(new BigDecimal("100.00"));
        invoice.setAmountReceived(BigDecimal.ZERO);
        invoice.setCreatedBy("tester");
        invoice.setCreatedAt(Instant.now());
        return invoice;
    }

    @Test
    void savesAndReloadsInvoiceWithLines() {
        UUID customerId = UUID.randomUUID();
        UUID salesOrderId = UUID.randomUUID();
        Invoice invoice = invoice(customerId, InvoiceKind.SALES_ORDER, InvoiceStatus.DRAFT, "ARI-202611-0001");
        invoice.setSalesOrderId(salesOrderId);
        invoice.setDueDate(LocalDate.of(2026, 12, 5));
        invoice.setNotes("first billing");

        InvoiceLine first = new InvoiceLine();
        first.setLineNumber(1);
        first.setSalesOrderItemId(UUID.randomUUID());
        first.setProductId(UUID.randomUUID());
        first.setQuantity(new BigDecimal("4.00"));
        first.setUnitPrice(new BigDecimal("10.00"));
        first.setLineTotal(new BigDecimal("40.00"));

        InvoiceLine second = new InvoiceLine();
        second.setLineNumber(2);
        second.setSalesOrderItemId(UUID.randomUUID());
        second.setProductId(UUID.randomUUID());
        second.setQuantity(new BigDecimal("3.00"));
        second.setUnitPrice(new BigDecimal("20.00"));
        second.setLineTotal(new BigDecimal("60.00"));
        invoice.setLines(List.of(first, second));

        Invoice saved = invoiceRepository.save(invoice);
        Invoice reloaded = invoiceRepository.findById(saved.getId()).orElseThrow();

        assertEquals("ARI-202611-0001", reloaded.getInvoiceNumber());
        assertEquals(InvoiceKind.SALES_ORDER, reloaded.getKind());
        assertEquals(customerId, reloaded.getCustomerId());
        assertEquals(salesOrderId, reloaded.getSalesOrderId());
        assertEquals(LocalDate.of(2026, 12, 5), reloaded.getDueDate());
        assertEquals(new BigDecimal("100.00"), reloaded.getTotal());
        assertEquals(2, reloaded.getLines().size());
        assertEquals(1, reloaded.getLines().get(0).getLineNumber());
        assertEquals(new BigDecimal("10.00"), reloaded.getLines().get(0).getUnitPrice());
        assertEquals(new BigDecimal("60.00"), reloaded.getLines().get(1).getLineTotal());
        assertEquals(first.getSalesOrderItemId(), reloaded.getLines().get(0).getSalesOrderItemId());
        assertEquals(1, invoiceRepository.findBySalesOrderId(salesOrderId).size());
        assertTrue(invoiceRepository.existsByInvoiceNumber("ARI-202611-0001"));
        assertTrue(invoiceRepository.findByIdForUpdate(saved.getId()).isPresent());
    }

    @Test
    void searchIsNullSafe() {
        UUID customerId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Invoice billing = invoice(customerId, InvoiceKind.PROGRESS_BILLING, InvoiceStatus.DRAFT, "ARI-202611-0002");
        billing.setProjectId(projectId);
        invoiceRepository.save(billing);
        invoiceRepository.save(invoice(customerId, InvoiceKind.SALES_ORDER, InvoiceStatus.DRAFT, "ARI-202611-0003"));

        assertEquals(2, invoiceRepository.search(null, null, null).size());
        assertEquals(1, invoiceRepository.search(customerId, InvoiceStatus.DRAFT, InvoiceKind.PROGRESS_BILLING).size());
        assertEquals(0, invoiceRepository.search(UUID.randomUUID(), null, null).size());
        assertEquals(1, invoiceRepository.findByProjectId(projectId).size());
    }

    @Test
    void findOpenReturnsApprovedAndPartiallyPaidOnly() {
        UUID customerId = UUID.randomUUID();
        invoiceRepository.save(invoice(customerId, InvoiceKind.SALES_ORDER, InvoiceStatus.DRAFT, "ARI-202611-0004"));
        invoiceRepository.save(invoice(customerId, InvoiceKind.SALES_ORDER, InvoiceStatus.APPROVED, "ARI-202611-0005"));
        invoiceRepository.save(
                invoice(customerId, InvoiceKind.SALES_ORDER, InvoiceStatus.PARTIALLY_PAID, "ARI-202611-0006"));
        invoiceRepository.save(invoice(customerId, InvoiceKind.SALES_ORDER, InvoiceStatus.PAID, "ARI-202611-0007"));

        assertEquals(2, invoiceRepository.findOpen().size());
    }

    @Test
    void savesAndReloadsReceiptWithAllocations() {
        UUID customerId = UUID.randomUUID();
        Invoice saved = invoiceRepository
                .save(invoice(customerId, InvoiceKind.SALES_ORDER, InvoiceStatus.APPROVED, "ARI-202611-0008"));

        Receipt receipt = new Receipt();
        receipt.setReceiptNumber("RCT-202611-0001");
        receipt.setCustomerId(customerId);
        receipt.setReceiptDate(LocalDate.of(2026, 11, 10));
        receipt.setMethod(PaymentMethod.BANK);
        receipt.setReference("deposit 55");
        receipt.setTotal(new BigDecimal("100.00"));
        receipt.setCreatedBy("tester");
        receipt.setCreatedAt(Instant.now());

        ReceiptAllocation allocation = new ReceiptAllocation();
        allocation.setInvoiceId(saved.getId());
        allocation.setAmount(new BigDecimal("100.00"));
        receipt.setAllocations(List.of(allocation));

        Receipt savedReceipt = receiptRepository.save(receipt);
        Receipt reloaded = receiptRepository.findById(savedReceipt.getId()).orElseThrow();

        assertEquals("RCT-202611-0001", reloaded.getReceiptNumber());
        assertEquals(PaymentMethod.BANK, reloaded.getMethod());
        assertEquals(1, reloaded.getAllocations().size());
        assertEquals(saved.getId(), reloaded.getAllocations().get(0).getInvoiceId());
        assertEquals(new BigDecimal("100.00"), reloaded.getAllocations().get(0).getAmount());
        assertEquals(1, receiptRepository.findByCustomerId(customerId).size());
        assertEquals(1, receiptRepository.findAll().size());
        assertTrue(receiptRepository.existsByReceiptNumber("RCT-202611-0001"));
    }
}
