package com.maiu.erp.modules.inventory.domain.model;

import java.time.Instant;
import java.util.UUID;

public class MaterialReturn {
    private UUID id;
    private String returnNumber;
    private UUID materialIssueId;
    private UUID projectId;
    private UUID warehouseId;
    private boolean reversal;
    private String remarks;
    private String performedBy;
    private Instant returnedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReturnNumber() {
        return returnNumber;
    }

    public void setReturnNumber(String returnNumber) {
        this.returnNumber = returnNumber;
    }

    public UUID getMaterialIssueId() {
        return materialIssueId;
    }

    public void setMaterialIssueId(UUID materialIssueId) {
        this.materialIssueId = materialIssueId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(UUID warehouseId) {
        this.warehouseId = warehouseId;
    }

    public boolean isReversal() {
        return reversal;
    }

    public void setReversal(boolean reversal) {
        this.reversal = reversal;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(Instant returnedAt) {
        this.returnedAt = returnedAt;
    }
}
