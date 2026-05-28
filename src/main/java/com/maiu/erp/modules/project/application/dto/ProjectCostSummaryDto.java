package com.maiu.erp.modules.project.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ProjectCostSummaryDto {
    private final UUID projectId;
    private final String projectName;
    private final BigDecimal budgetAmount;
    private final BigDecimal totalBudgetLineAmount;
    private final BigDecimal budgetBasisAmount;
    private final BigDecimal totalPurchaseOrderAmount;
    private final BigDecimal totalMaterialIssuedCost;
    private final BigDecimal totalMaterialReturnedCost;
    private final BigDecimal netMaterialIssuedCost;
    private final BigDecimal committedCost;
    private final BigDecimal actualCost;
    private final BigDecimal committedVariance;
    private final BigDecimal actualVariance;
    private final BigDecimal budgetVariance;
    private final List<ProjectCostItemDto> items;

    public ProjectCostSummaryDto(
            UUID projectId,
            String projectName,
            BigDecimal budgetAmount,
            BigDecimal totalBudgetLineAmount,
            BigDecimal budgetBasisAmount,
            BigDecimal totalPurchaseOrderAmount,
            BigDecimal totalMaterialIssuedCost,
            BigDecimal totalMaterialReturnedCost,
            BigDecimal netMaterialIssuedCost,
            BigDecimal committedCost,
            BigDecimal actualCost,
            BigDecimal committedVariance,
            BigDecimal actualVariance,
            BigDecimal budgetVariance,
            List<ProjectCostItemDto> items) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.budgetAmount = budgetAmount;
        this.totalBudgetLineAmount = totalBudgetLineAmount;
        this.budgetBasisAmount = budgetBasisAmount;
        this.totalPurchaseOrderAmount = totalPurchaseOrderAmount;
        this.totalMaterialIssuedCost = totalMaterialIssuedCost;
        this.totalMaterialReturnedCost = totalMaterialReturnedCost;
        this.netMaterialIssuedCost = netMaterialIssuedCost;
        this.committedCost = committedCost;
        this.actualCost = actualCost;
        this.committedVariance = committedVariance;
        this.actualVariance = actualVariance;
        this.budgetVariance = budgetVariance;
        this.items = items;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public BigDecimal getTotalBudgetLineAmount() {
        return totalBudgetLineAmount;
    }

    public BigDecimal getBudgetBasisAmount() {
        return budgetBasisAmount;
    }

    public BigDecimal getTotalPurchaseOrderAmount() {
        return totalPurchaseOrderAmount;
    }

    public BigDecimal getTotalMaterialIssuedCost() {
        return totalMaterialIssuedCost;
    }

    public BigDecimal getTotalMaterialReturnedCost() {
        return totalMaterialReturnedCost;
    }

    public BigDecimal getNetMaterialIssuedCost() {
        return netMaterialIssuedCost;
    }

    public BigDecimal getCommittedCost() {
        return committedCost;
    }

    public BigDecimal getActualCost() {
        return actualCost;
    }

    public BigDecimal getCommittedVariance() {
        return committedVariance;
    }

    public BigDecimal getActualVariance() {
        return actualVariance;
    }

    public BigDecimal getBudgetVariance() {
        return budgetVariance;
    }

    public List<ProjectCostItemDto> getItems() {
        return items;
    }
}
