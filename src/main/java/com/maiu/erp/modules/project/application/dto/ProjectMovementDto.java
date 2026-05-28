package com.maiu.erp.modules.project.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.enums.MovementType;

public class ProjectMovementDto {
    private final UUID id;
    private final UUID productId;
    private final UUID warehouseId;
    private final MovementType movementType;
    private final BigDecimal quantity;
    private final BigDecimal unitCost;
    private final String referenceType;
    private final UUID referenceId;
    private final boolean reversal;
    private final String remarks;
    private final String performedBy;
    private final Instant movementDate;

    public ProjectMovementDto(
            UUID id,
            UUID productId,
            UUID warehouseId,
            MovementType movementType,
            BigDecimal quantity,
            BigDecimal unitCost,
            String referenceType,
            UUID referenceId,
            boolean reversal,
            String remarks,
            String performedBy,
            Instant movementDate) {
        this.id = id;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.reversal = reversal;
        this.remarks = remarks;
        this.performedBy = performedBy;
        this.movementDate = movementDate;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public UUID getWarehouseId() { return warehouseId; }
    public MovementType getMovementType() { return movementType; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public String getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public boolean isReversal() { return reversal; }
    public String getRemarks() { return remarks; }
    public String getPerformedBy() { return performedBy; }
    public Instant getMovementDate() { return movementDate; }
}
