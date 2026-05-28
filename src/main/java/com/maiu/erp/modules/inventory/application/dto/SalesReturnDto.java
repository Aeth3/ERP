package com.maiu.erp.modules.inventory.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SalesReturnDto {
    private final UUID id;
    private final String returnNumber;
    private final UUID salesOrderId;
    private final UUID customerId;
    private final UUID warehouseId;
    private final String remarks;
    private final String performedBy;
    private final Instant returnedAt;
    private final List<SalesReturnItemDto> items;

    public SalesReturnDto(
            UUID id,
            String returnNumber,
            UUID salesOrderId,
            UUID customerId,
            UUID warehouseId,
            String remarks,
            String performedBy,
            Instant returnedAt,
            List<SalesReturnItemDto> items) {
        this.id = id;
        this.returnNumber = returnNumber;
        this.salesOrderId = salesOrderId;
        this.customerId = customerId;
        this.warehouseId = warehouseId;
        this.remarks = remarks;
        this.performedBy = performedBy;
        this.returnedAt = returnedAt;
        this.items = items;
    }

    public UUID getId() { return id; }
    public String getReturnNumber() { return returnNumber; }
    public UUID getSalesOrderId() { return salesOrderId; }
    public UUID getCustomerId() { return customerId; }
    public UUID getWarehouseId() { return warehouseId; }
    public String getRemarks() { return remarks; }
    public String getPerformedBy() { return performedBy; }
    public Instant getReturnedAt() { return returnedAt; }
    public List<SalesReturnItemDto> getItems() { return items; }
}
