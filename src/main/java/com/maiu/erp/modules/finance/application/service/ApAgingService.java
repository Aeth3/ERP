package com.maiu.erp.modules.finance.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.finance.domain.model.Bill;
import com.maiu.erp.modules.finance.domain.repository.BillRepository;

/** Open AP balances per supplier, bucketed by days past due. */
@Service
public class ApAgingService {
    private final BillRepository billRepository;
    private final ZoneId businessZone;

    public ApAgingService(
            BillRepository billRepository,
            @Value("${finance.time-zone:Asia/Manila}") String businessZone) {
        this.billRepository = billRepository;
        this.businessZone = ZoneId.of(businessZone);
    }

    public record AgingRow(
            UUID supplierId,
            BigDecimal current,
            BigDecimal days1To30,
            BigDecimal days31To60,
            BigDecimal days61To90,
            BigDecimal over90,
            BigDecimal total) {
    }

    public List<AgingRow> aging() {
        return aging(LocalDate.now(businessZone));
    }

    public List<AgingRow> aging(LocalDate asOf) {
        Map<UUID, BigDecimal[]> buckets = new LinkedHashMap<>();
        for (Bill bill : billRepository.findOpen()) {
            BigDecimal paid = bill.getAmountPaid() == null ? BigDecimal.ZERO : bill.getAmountPaid();
            BigDecimal due = bill.getTotal().subtract(paid);
            if (due.signum() <= 0) {
                continue;
            }
            LocalDate dueDate = bill.getDueDate() != null ? bill.getDueDate() : bill.getBillDate();
            long daysPastDue = ChronoUnit.DAYS.between(dueDate, asOf);
            int bucket;
            if (daysPastDue <= 0) {
                bucket = 0;
            } else if (daysPastDue <= 30) {
                bucket = 1;
            } else if (daysPastDue <= 60) {
                bucket = 2;
            } else if (daysPastDue <= 90) {
                bucket = 3;
            } else {
                bucket = 4;
            }
            BigDecimal[] amounts = buckets.computeIfAbsent(bill.getSupplierId(), id -> new BigDecimal[] {
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
            amounts[bucket] = amounts[bucket].add(due);
        }

        List<AgingRow> rows = new ArrayList<>();
        buckets.forEach((supplierId, amounts) -> rows.add(new AgingRow(
                supplierId, amounts[0], amounts[1], amounts[2], amounts[3], amounts[4],
                amounts[0].add(amounts[1]).add(amounts[2]).add(amounts[3]).add(amounts[4]))));
        rows.sort(Comparator.comparing(AgingRow::total).reversed());
        return rows;
    }
}
