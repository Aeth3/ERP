package com.maiu.erp.modules.reporting.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ReportMovementItemDto {
    private final UUID id;
    private final String movementType;
    private final BigDecimal quantity;
    private final BigDecimal unitCost;
    private final String referenceType;
    private final UUID referenceId;
    private final String remarks;
    private final String performedBy;
    private final Instant movementDate;
    private final UUID productId;
    private final String productName;
    private final String productSku;
    private final UUID warehouseId;
    private final String warehouseName;
    private final String warehouseCode;
    private final UUID projectId;
    private final String projectCode;
    private final String projectName;

    public ReportMovementItemDto(
            UUID id,
            String movementType,
            BigDecimal quantity,
            BigDecimal unitCost,
            String referenceType,
            UUID referenceId,
            String remarks,
            String performedBy,
            Instant movementDate,
            UUID productId,
            String productName,
            String productSku,
            UUID warehouseId,
            String warehouseName,
            String warehouseCode,
            UUID projectId,
            String projectCode,
            String projectName) {
        this.id = id;
        this.movementType = movementType;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.remarks = remarks;
        this.performedBy = performedBy;
        this.movementDate = movementDate;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.warehouseCode = warehouseCode;
        this.projectId = projectId;
        this.projectCode = projectCode;
        this.projectName = projectName;
    }

    public UUID getId() {
        return id;
    }

    public String getMovementType() {
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

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductSku() {
        return productSku;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public String getProjectName() {
        return projectName;
    }
}
