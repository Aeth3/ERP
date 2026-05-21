package com.maiu.erp.modules.inventory.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class InventoryStock {
    private UUID id;
    private UUID productId;
    private UUID warehouseId;
    private BigDecimal quantityOnHand;
    private BigDecimal reservedQuantity;
    private BigDecimal reorderLevel;
    private Instant updatedAt;

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public void setWarehouseId(UUID warehouseId) {
        this.warehouseId = warehouseId;
    }

    public void setQuantityOnHand(BigDecimal quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public void setReservedQuantity(BigDecimal reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public BigDecimal getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public BigDecimal getReservedQuantity() {
        return reservedQuantity;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setReorderLevel(BigDecimal reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
