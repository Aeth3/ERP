package com.maiu.erp.modules.reporting.application.dto;

import java.time.Instant;
import java.util.UUID;

public class ReportReturnDto {
    private final UUID id;
    private final String returnNumber;
    private final UUID warehouseId;
    private final String warehouseCode;
    private final String warehouseName;
    private final String remarks;
    private final String performedBy;
    private final Instant returnedAt;

    public ReportReturnDto(
            UUID id,
            String returnNumber,
            UUID warehouseId,
            String warehouseCode,
            String warehouseName,
            String remarks,
            String performedBy,
            Instant returnedAt) {
        this.id = id;
        this.returnNumber = returnNumber;
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.warehouseName = warehouseName;
        this.remarks = remarks;
        this.performedBy = performedBy;
        this.returnedAt = returnedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getReturnNumber() {
        return returnNumber;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }
}
