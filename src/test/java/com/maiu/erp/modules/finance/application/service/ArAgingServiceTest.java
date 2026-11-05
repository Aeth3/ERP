package com.maiu.erp.modules.finance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.finance.application.service.ArAgingService.AgingRow;
import com.maiu.erp.modules.finance.domain.enums.InvoiceKind;
import com.maiu.erp.modules.finance.domain.enums.InvoiceStatus;
import com.maiu.erp.modules.finance.domain.model.Invoice;
import com.maiu.erp.modules.finance.testsupport.InMemoryInvoiceRepository;

class ArAgingServiceTest {
    private static final LocalDate AS_OF = LocalDate.of(2026, 11, 5);

    private InMemoryInvoiceRepository invoiceRepository;
    private ArAgingService service;

    @BeforeEach
    void setUp() {
        invoiceRepository = new InMemoryInvoiceRepository();
        service = new ArAgingService(invoiceRepository, "Asia/Manila");
    }

    private Invoice invoice(UUID customerId, String total, LocalDate invoiceDate, LocalDate dueDate) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(UUID.randomUUID().toString());
        invoice.setKind(InvoiceKind.SALES_ORDER);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceDate(invoiceDate);
        invoice.setDueDate(dueDate);
        invoice.setStatus(InvoiceStatus.APPROVED);
        invoice.setSubtotal(new BigDecimal(total));
        invoice.setRetentionAmount(BigDecimal.ZERO);
        invoice.setTotal(new BigDecimal(total));
        invoice.setAmountReceived(BigDecimal.ZERO);
        invoice.setCreatedBy("tester");
        invoice.setCreatedAt(Instant.now());
        return invoiceRepository.save(invoice);
    }

    @Test
    void bucketsByDaysOverdue() {
        UUID customerId = UUID.randomUUID();
        invoice(customerId, "10.00", AS_OF.minusDays(5), AS_OF.plusDays(10)); // current
        invoice(customerId, "20.00", AS_OF.minusDays(40), AS_OF.minusDays(15)); // 1-30
        invoice(customerId, "30.00", AS_OF.minusDays(80), AS_OF.minusDays(45)); // 31-60
        invoice(customerId, "40.00", AS_OF.minusDays(120), AS_OF.minusDays(75)); // 61-90
        invoice(customerId, "50.00", AS_OF.minusDays(200), AS_OF.minusDays(120)); // over 90

        List<AgingRow> rows = service.aging(AS_OF);

        assertEquals(1, rows.size());
        AgingRow row = rows.get(0);
        assertEquals(new BigDecimal("10.00"), row.current());
        assertEquals(new BigDecimal("20.00"), row.days1To30());
        assertEquals(new BigDecimal("30.00"), row.days31To60());
        assertEquals(new BigDecimal("40.00"), row.days61To90());
        assertEquals(new BigDecimal("50.00"), row.over90());
        assertEquals(new BigDecimal("150.00"), row.total());
    }

    @Test
    void fallsBackToInvoiceDateWhenNoDueDate() {
        UUID customerId = UUID.randomUUID();
        invoice(customerId, "25.00", AS_OF.minusDays(45), null);

        AgingRow row = service.aging(AS_OF).get(0);
        assertEquals(new BigDecimal("25.00"), row.days31To60());
    }

    @Test
    void netsAmountReceived() {
        UUID customerId = UUID.randomUUID();
        Invoice partiallyPaid = invoice(customerId, "100.00", AS_OF.minusDays(10), AS_OF.minusDays(5));
        partiallyPaid.setStatus(InvoiceStatus.PARTIALLY_PAID);
        partiallyPaid.setAmountReceived(new BigDecimal("70.00"));
        invoiceRepository.save(partiallyPaid);

        AgingRow row = service.aging(AS_OF).get(0);
        assertEquals(new BigDecimal("30.00"), row.days1To30());
        assertEquals(new BigDecimal("30.00"), row.total());
    }
}
