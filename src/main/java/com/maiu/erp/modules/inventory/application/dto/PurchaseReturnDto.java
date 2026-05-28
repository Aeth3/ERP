package com.maiu.erp.modules.inventory.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PurchaseReturnDto {
    private final UUID id;
    private final String returnNumber;
    private final UUID purchaseOrderId;
    private final UUID supplierId;
    private final UUID warehouseId;
    private final String remarks;
    private final String performedBy;
    private final Instant returnedAt;
    private final List<PurchaseReturnItemDto> items;

    public PurchaseReturnDto(
            UUID id,
            String returnNumber,
            UUID purchaseOrderId,
            UUID supplierId,
            UUID warehouseId,
            String remarks,
            String performedBy,
            Instant returnedAt,
            List<PurchaseReturnItemDto> items) {
        this.id = id;
        this.returnNumber = returnNumber;
        this.purchaseOrderId = purchaseOrderId;
        this.supplierId = supplierId;
        this.warehouseId = warehouseId;
        this.remarks = remarks;
        this.performedBy = performedBy;
        this.returnedAt = returnedAt;
        this.items = items;
    }

    public UUID getId() { return id; }
    public String getReturnNumber() { return returnNumber; }
    public UUID getPurchaseOrderId() { return purchaseOrderId; }
    public UUID getSupplierId() { return supplierId; }
    public UUID getWarehouseId() { return warehouseId; }
    public String getRemarks() { return remarks; }
    public String getPerformedBy() { return performedBy; }
    public Instant getReturnedAt() { return returnedAt; }
    public List<PurchaseReturnItemDto> getItems() { return items; }
}
