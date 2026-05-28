package com.maiu.erp.modules.inventory.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class MaterialReturnItem {
    private UUID id;
    private UUID materialReturnId;
    private UUID productId;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal lineTotal;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMaterialReturnId() {
        return materialReturnId;
    }

    public void setMaterialReturnId(UUID materialReturnId) {
        this.materialReturnId = materialReturnId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
