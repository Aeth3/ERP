package com.maiu.erp.modules.inventory.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class SalesOrderItem {
    private UUID id;
    private UUID salesOrderId;
    private UUID productId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public UUID getProductId() {
        return productId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSalesOrderId() {
        return salesOrderId;
    }

    public void setSalesOrderId(UUID salesOrderId) {
        this.salesOrderId = salesOrderId;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
