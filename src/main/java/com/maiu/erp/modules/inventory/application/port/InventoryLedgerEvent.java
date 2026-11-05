package com.maiu.erp.modules.inventory.application.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A valued stock change handed to the general ledger. Amount = quantity x unitCost.
 * sourceId is the business document (PO, issue, return); stockMovementId keeps postings idempotent.
 */
public record InventoryLedgerEvent(
        Kind kind,
        UUID stockMovementId,
        UUID sourceId,
        UUID productId,
        UUID warehouseId,
        UUID projectId,
        BigDecimal quantity,
        BigDecimal unitCost,
        String memo,
        String performedBy,
        Instant occurredAt) {

    public enum Kind {
        PURCHASE_RECEIPT,
        PURCHASE_RETURN,
        PROJECT_ISSUE,
        PROJECT_RETURN,
        ADJUSTMENT_INCREASE,
        ADJUSTMENT_DECREASE,
        SALES_SHIPMENT,
        SALES_RETURN
    }
}
