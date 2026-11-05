package com.maiu.erp.modules.finance.application.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Finance's read-only view of a purchase order for 3-way matching. Implemented by the
 * inventory module; status is the PO status name so finance stays decoupled from
 * inventory enums.
 */
public interface PurchaseOrderReader {
    Optional<PoSnapshot> find(UUID purchaseOrderId);

    record PoSnapshot(UUID id, String poNumber, UUID supplierId, String status, List<PoLine> lines) {
    }

    record PoLine(UUID poItemId, UUID productId, BigDecimal quantity, BigDecimal returnedQuantity,
            BigDecimal unitCost) {
    }
}
