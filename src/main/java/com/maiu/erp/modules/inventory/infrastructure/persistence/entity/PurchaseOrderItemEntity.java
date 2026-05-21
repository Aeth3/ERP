package com.maiu.erp.modules.inventory.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID purchaseOrderId;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal unitCost;

    @Column(nullable = false)
    private BigDecimal lineTotal;
    // =========================
    // GETTERS AND SETTERS
    // =========================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(UUID purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
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

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}