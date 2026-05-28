package com.maiu.erp.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class SalesReturnItemDto {
    private final UUID id;
    private final UUID salesReturnId;
    private final UUID productId;
    private final BigDecimal quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal lineTotal;

    public SalesReturnItemDto(
            UUID id,
            UUID salesReturnId,
            UUID productId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {
        this.id = id;
        this.salesReturnId = salesReturnId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    public UUID getId() { return id; }
    public UUID getSalesReturnId() { return salesReturnId; }
    public UUID getProductId() { return productId; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
