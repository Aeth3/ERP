package com.maiu.erp.modules.inventory.domain.model;

import java.util.UUID;
import java.math.BigDecimal;

public class PurchaseOrderItem {
    private UUID id;
    private UUID purchaseOrderId;
    private UUID productId;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal lineTotal;

    public UUID getProductId() {
        return productId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setId(UUID id) {
this.id = id;
    }

    public void setPurchaseOrderId(UUID purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public UUID getId() {
    return id;
    }

    public UUID getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
