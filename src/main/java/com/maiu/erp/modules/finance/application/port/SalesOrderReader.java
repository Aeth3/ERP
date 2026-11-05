package com.maiu.erp.modules.finance.application.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Finance's window into sales orders, implemented by the inventory module. Shipping is
 * whole-order, so a shipped order's full item quantities are billable net of returns.
 */
public interface SalesOrderReader {

    Optional<SoSnapshot> find(UUID salesOrderId);

    record SoSnapshot(UUID id, String soNumber, UUID customerId, String status, List<SoLine> lines) {
    }

    record SoLine(
            UUID soItemId,
            UUID productId,
            String productName,
            BigDecimal quantity,
            BigDecimal returnedQuantity,
            BigDecimal unitPrice) {
    }
}
