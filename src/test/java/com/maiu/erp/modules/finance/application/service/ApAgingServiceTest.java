package com.maiu.erp.modules.finance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.finance.application.service.ApAgingService.AgingRow;
import com.maiu.erp.modules.finance.domain.enums.BillStatus;
import com.maiu.erp.modules.finance.domain.model.Bill;
import com.maiu.erp.modules.finance.testsupport.InMemoryBillRepository;

class ApAgingServiceTest {
    private static final LocalDate AS_OF = LocalDate.of(2026, 11, 5);

    private InMemoryBillRepository billRepository;
    private ApAgingService service;

    @BeforeEach
    void setUp() {
        billRepository = new InMemoryBillRepository();
        service = new ApAgingService(billRepository, "Asia/Manila");
    }

    @Test
    void bucketsOpenBalanceByDaysPastDue() {
        UUID supplierId = UUID.randomUUID();
        // Due today -> CURRENT.
        bill(supplierId, "100.00", "0.00", AS_OF, BillStatus.APPROVED);
        // 30 days past due -> D1_30 boundary.
        bill(supplierId, "200.00", "50.00", AS_OF.minusDays(30), BillStatus.PARTIALLY_PAID);
        // 31 days past due -> D31_60 boundary.
        bill(supplierId, "300.00", "0.00", AS_OF.minusDays(31), BillStatus.APPROVED);
        // 95 days past due -> OVER_90.
        bill(supplierId, "400.00", "0.00", AS_OF.minusDays(95), BillStatus.APPROVED);
        // Paid bills are excluded entirely.
        bill(supplierId, "999.00", "999.00", AS_OF.minusDays(95), BillStatus.PAID);

        List<AgingRow> rows = service.aging(AS_OF);

        assertEquals(1, rows.size());
        AgingRow row = rows.get(0);
        assertEquals(supplierId, row.supplierId());
        assertEquals(new BigDecimal("100.00"), row.current());
        assertEquals(new BigDecimal("150.00"), row.days1To30());
        assertEquals(new BigDecimal("300.00"), row.days31To60());
        assertEquals(BigDecimal.ZERO, row.days61To90());
        assertEquals(new BigDecimal("400.00"), row.over90());
        assertEquals(new BigDecimal("950.00"), row.total());
    }

    @Test
    void nullDueDateFallsBackToBillDate() {
        UUID supplierId = UUID.randomUUID();
        Bill bill = bill(supplierId, "80.00", "0.00", null, BillStatus.APPROVED);
        bill.setBillDate(AS_OF.minusDays(40));
        billRepository.save(bill);

        List<AgingRow> rows = service.aging(AS_OF);

        assertEquals(new BigDecimal("80.00"), rows.get(0).days31To60());
    }

    private Bill bill(UUID supplierId, String total, String paid, LocalDate dueDate, BillStatus status) {
        Bill bill = new Bill();
        bill.setBillNumber("INV-" + UUID.randomUUID().toString().substring(0, 6));
        bill.setSupplierId(supplierId);
        bill.setBillDate(AS_OF.minusDays(5));
        bill.setDueDate(dueDate);
        bill.setStatus(status);
        bill.setSubtotal(new BigDecimal(total));
        bill.setTotal(new BigDecimal(total));
        bill.setAmountPaid(new BigDecimal(paid));
        bill.setCreatedBy("tester");
        bill.setCreatedAt(Instant.now());
        return billRepository.save(bill);
    }
}
