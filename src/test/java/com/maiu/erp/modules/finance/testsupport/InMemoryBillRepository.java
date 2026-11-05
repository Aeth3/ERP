package com.maiu.erp.modules.finance.testsupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.enums.BillStatus;
import com.maiu.erp.modules.finance.domain.model.Bill;
import com.maiu.erp.modules.finance.domain.repository.BillRepository;

public final class InMemoryBillRepository implements BillRepository {
    private final List<Bill> bills = new ArrayList<>();
    private int forUpdateCalls;

    public int getForUpdateCalls() {
        return forUpdateCalls;
    }

    @Override
    public Bill save(Bill bill) {
        if (bill.getId() == null) {
            bill.setId(UUID.randomUUID());
            bills.add(bill);
        } else if (bills.stream().noneMatch(existing -> bill.getId().equals(existing.getId()))) {
            bills.add(bill);
        } else {
            bills.replaceAll(existing -> bill.getId().equals(existing.getId()) ? bill : existing);
        }
        return bill;
    }

    @Override
    public Optional<Bill> findById(UUID id) {
        return bills.stream().filter(bill -> id.equals(bill.getId())).findFirst();
    }

    @Override
    public Optional<Bill> findByIdForUpdate(UUID id) {
        forUpdateCalls++;
        return findById(id);
    }

    @Override
    public List<Bill> findByPurchaseOrderId(UUID purchaseOrderId) {
        return bills.stream().filter(bill -> purchaseOrderId.equals(bill.getPurchaseOrderId())).toList();
    }

    @Override
    public List<Bill> search(UUID supplierId, BillStatus status, UUID purchaseOrderId) {
        return bills.stream()
                .filter(bill -> supplierId == null || supplierId.equals(bill.getSupplierId()))
                .filter(bill -> status == null || status == bill.getStatus())
                .filter(bill -> purchaseOrderId == null || purchaseOrderId.equals(bill.getPurchaseOrderId()))
                .toList();
    }

    @Override
    public boolean existsBySupplierIdAndBillNumberIgnoreCaseAndStatusNot(UUID supplierId, String billNumber,
            BillStatus excluded) {
        return bills.stream().anyMatch(bill -> supplierId.equals(bill.getSupplierId())
                && billNumber.equalsIgnoreCase(bill.getBillNumber())
                && bill.getStatus() != excluded);
    }

    @Override
    public List<Bill> findOpen() {
        return bills.stream()
                .filter(bill -> bill.getStatus() == BillStatus.APPROVED || bill.getStatus() == BillStatus.PARTIALLY_PAID)
                .toList();
    }
}
