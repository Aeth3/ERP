package com.maiu.erp.modules.inventory.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.enums.MovementType;

public class StockMovement {
    private UUID id;
    private UUID productId;
    private UUID warehouseId;
    private UUID projectId;
    private MovementType movementType;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private String referenceType;
    private UUID referenceId;
    private String remarks;
    private Instant movementDate;
    private String performedBy;

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public void setWarehouseId(UUID warehouseId) {
        this.warehouseId = warehouseId;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public void setMovementDate(Instant movementDate) {
        this.movementDate = movementDate;
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

    public MovementType getMovementType() {
        return movementType;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Instant getMovementDate() {
        return movementDate;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getRemarks() {
        return remarks;
    }
}
