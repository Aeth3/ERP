package com.maiu.erp.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PurchaseReturnItemDto {
    private final UUID id;
    private final UUID purchaseReturnId;
    private final UUID productId;
    private final BigDecimal quantity;
    private final BigDecimal unitCost;
    private final BigDecimal lineTotal;

    public PurchaseReturnItemDto(
            UUID id,
            UUID purchaseReturnId,
            UUID productId,
            BigDecimal quantity,
            BigDecimal unitCost,
            BigDecimal lineTotal) {
        this.id = id;
        this.purchaseReturnId = purchaseReturnId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.lineTotal = lineTotal;
    }

    public UUID getId() { return id; }
    public UUID getPurchaseReturnId() { return purchaseReturnId; }
    public UUID getProductId() { return productId; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
