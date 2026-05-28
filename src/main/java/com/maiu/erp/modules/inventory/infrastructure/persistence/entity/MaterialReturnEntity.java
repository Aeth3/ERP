package com.maiu.erp.modules.inventory.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "material_returns")
public class MaterialReturnEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String returnNumber;

    @Column(nullable = false)
    private UUID materialIssueId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private UUID warehouseId;

    @Column(nullable = false)
    private boolean reversal;

    private String remarks;

    @Column(nullable = false)
    private String performedBy;

    @Column(nullable = false)
    private Instant returnedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getReturnNumber() { return returnNumber; }
    public void setReturnNumber(String returnNumber) { this.returnNumber = returnNumber; }
    public UUID getMaterialIssueId() { return materialIssueId; }
    public void setMaterialIssueId(UUID materialIssueId) { this.materialIssueId = materialIssueId; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }
    public boolean isReversal() { return reversal; }
    public void setReversal(boolean reversal) { this.reversal = reversal; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public Instant getReturnedAt() { return returnedAt; }
    public void setReturnedAt(Instant returnedAt) { this.returnedAt = returnedAt; }
}
