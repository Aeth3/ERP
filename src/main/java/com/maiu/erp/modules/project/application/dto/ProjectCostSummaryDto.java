package com.maiu.erp.modules.project.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ProjectCostSummaryDto {
    private final UUID projectId;
    private final String projectName;
    private final BigDecimal totalPurchaseOrderAmount;
    private final BigDecimal totalMaterialIssuedCost;
    private final List<ProjectCostItemDto> items;

    public ProjectCostSummaryDto(
            UUID projectId,
            String projectName,
            BigDecimal totalPurchaseOrderAmount,
            BigDecimal totalMaterialIssuedCost,
            List<ProjectCostItemDto> items) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.totalPurchaseOrderAmount = totalPurchaseOrderAmount;
        this.totalMaterialIssuedCost = totalMaterialIssuedCost;
        this.items = items;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public BigDecimal getTotalPurchaseOrderAmount() {
        return totalPurchaseOrderAmount;
    }

    public BigDecimal getTotalMaterialIssuedCost() {
        return totalMaterialIssuedCost;
    }

    public List<ProjectCostItemDto> getItems() {
        return items;
    }
}
