package com.maiu.erp.modules.inventory.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MaterialReturnDto {
    private final UUID id;
    private final String returnNumber;
    private final UUID materialIssueId;
    private final UUID projectId;
    private final UUID warehouseId;
    private final boolean reversal;
    private final String remarks;
    private final String performedBy;
    private final Instant returnedAt;
    private final List<MaterialReturnItemDto> items;

    public MaterialReturnDto(
            UUID id,
            String returnNumber,
            UUID materialIssueId,
            UUID projectId,
            UUID warehouseId,
            boolean reversal,
            String remarks,
            String performedBy,
            Instant returnedAt,
            List<MaterialReturnItemDto> items) {
        this.id = id;
        this.returnNumber = returnNumber;
        this.materialIssueId = materialIssueId;
        this.projectId = projectId;
        this.warehouseId = warehouseId;
        this.reversal = reversal;
        this.remarks = remarks;
        this.performedBy = performedBy;
        this.returnedAt = returnedAt;
        this.items = items;
    }

    public UUID getId() { return id; }
    public String getReturnNumber() { return returnNumber; }
    public UUID getMaterialIssueId() { return materialIssueId; }
    public UUID getProjectId() { return projectId; }
    public UUID getWarehouseId() { return warehouseId; }
    public boolean isReversal() { return reversal; }
    public String getRemarks() { return remarks; }
    public String getPerformedBy() { return performedBy; }
    public Instant getReturnedAt() { return returnedAt; }
    public List<MaterialReturnItemDto> getItems() { return items; }
}
