package com.maiu.erp.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class InventoryStockDto {
    private final UUID id;
    private final UUID productId;
    private final UUID warehouseId;
    private final BigDecimal quantityOnHand;
    private final BigDecimal reservedQuantity;
    private final BigDecimal availableQuantity;
    private final BigDecimal reorderLevel;
    private final Instant updatedAt;

    public InventoryStockDto(
            UUID id,
            UUID productId,
            UUID warehouseId,
            BigDecimal quantityOnHand,
            BigDecimal reservedQuantity,
            BigDecimal availableQuantity,
            BigDecimal reorderLevel,
            Instant updatedAt) {
        this.id = id;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantityOnHand = quantityOnHand;
        this.reservedQuantity = reservedQuantity;
        this.availableQuantity = availableQuantity;
        this.reorderLevel = reorderLevel;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public BigDecimal getQuantityOnHand() {
        return quantityOnHand;
    }

    public BigDecimal getReservedQuantity() {
        return reservedQuantity;
    }

    public BigDecimal getAvailableQuantity() {
        return availableQuantity;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
