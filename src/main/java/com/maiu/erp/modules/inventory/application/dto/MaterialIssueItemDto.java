package com.maiu.erp.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class MaterialIssueItemDto {
    private final UUID id;
    private final UUID materialIssueId;
    private final UUID productId;
    private final BigDecimal quantity;
    private final BigDecimal unitCost;
    private final BigDecimal lineTotal;

    public MaterialIssueItemDto(
            UUID id,
            UUID materialIssueId,
            UUID productId,
            BigDecimal quantity,
            BigDecimal unitCost,
            BigDecimal lineTotal) {
        this.id = id;
        this.materialIssueId = materialIssueId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.lineTotal = lineTotal;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMaterialIssueId() {
        return materialIssueId;
    }

    public UUID getProductId() {
        return productId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
