package com.maiu.erp.modules.finance.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.enums.BillStatus;
import com.maiu.erp.modules.finance.domain.model.Bill;

public interface BillRepository {
    Bill save(Bill bill);

    Optional<Bill> findById(UUID id);

    List<Bill> findByPurchaseOrderId(UUID purchaseOrderId);

    /** All filters optional; null means "any". */
    List<Bill> search(UUID supplierId, BillStatus status, UUID purchaseOrderId);

    boolean existsBySupplierIdAndBillNumberAndStatusNot(UUID supplierId, String billNumber, BillStatus excluded);

    /** Bills with an outstanding balance: APPROVED or PARTIALLY_PAID. */
    List<Bill> findOpen();
}
