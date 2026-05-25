package com.maiu.erp.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.enums.MovementType;

public class StockMovementDto {

    private final UUID id;
    private final UUID productId;
    private final UUID warehouseId;
    private final UUID projectId;
    private final MovementType movementType;
    private final BigDecimal quantity;
    private final BigDecimal unitCost;
    private final String referenceType;
    private final UUID referenceId;
    private final String remarks;
    private final String performedBy;
    private final Instant movementDate;

    public StockMovementDto(
            UUID id,
            UUID productId,
            UUID warehouseId,
            UUID projectId,
            MovementType movementType,
            BigDecimal quantity,
            BigDecimal unitCost,
            String referenceType,
            UUID referenceId,
            String remarks,
            String performedBy,
            Instant movementDate) {
        this.id = id;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.projectId = projectId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.remarks = remarks;
        this.performedBy = performedBy;
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

    public UUID getProjectId() {
        return projectId;
    }

    public MovementType getMovementType() {
        return movementType;
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

    public String getRemarks() {
        return remarks;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Instant getMovementDate() {
        return movementDate;
    }
}
