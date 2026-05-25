package com.maiu.erp.modules.inventory.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MaterialIssueDto {
    private final UUID id;
    private final String issueNumber;
    private final UUID projectId;
    private final UUID warehouseId;
    private final String remarks;
    private final String performedBy;
    private final Instant issuedAt;
    private final List<MaterialIssueItemDto> items;

    public MaterialIssueDto(
            UUID id,
            String issueNumber,
            UUID projectId,
            UUID warehouseId,
            String remarks,
            String performedBy,
            Instant issuedAt,
            List<MaterialIssueItemDto> items) {
        this.id = id;
        this.issueNumber = issueNumber;
        this.projectId = projectId;
        this.warehouseId = warehouseId;
        this.remarks = remarks;
        this.performedBy = performedBy;
        this.issuedAt = issuedAt;
        this.items = items;
    }

    public UUID getId() {
        return id;
    }

    public String getIssueNumber() {
        return issueNumber;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public List<MaterialIssueItemDto> getItems() {
        return items;
    }
}
