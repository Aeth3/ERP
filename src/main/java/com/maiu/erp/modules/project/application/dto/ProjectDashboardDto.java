package com.maiu.erp.modules.project.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.maiu.erp.modules.project.domain.enums.ProjectStatus;

public class ProjectDashboardDto {
    private final UUID projectId;
    private final String projectName;
    private final ProjectStatus status;
    private final BigDecimal budgetBasisAmount;
    private final BigDecimal committedCost;
    private final BigDecimal actualCost;
    private final BigDecimal committedVariance;
    private final BigDecimal actualVariance;
    private final BigDecimal budgetUtilizationPercent;
    private final BigDecimal totalIssuedQuantity;
    private final BigDecimal totalReturnedQuantity;
    private final BigDecimal netIssuedQuantity;
    private final int totalIssueCount;
    private final int totalReturnCount;
    private final int totalReversalCount;
    private final int movementCount;
    private final Instant lastMovementDate;

    public ProjectDashboardDto(
            UUID projectId,
            String projectName,
            ProjectStatus status,
            BigDecimal budgetBasisAmount,
            BigDecimal committedCost,
            BigDecimal actualCost,
            BigDecimal committedVariance,
            BigDecimal actualVariance,
            BigDecimal budgetUtilizationPercent,
            BigDecimal totalIssuedQuantity,
            BigDecimal totalReturnedQuantity,
            BigDecimal netIssuedQuantity,
            int totalIssueCount,
            int totalReturnCount,
            int totalReversalCount,
            int movementCount,
            Instant lastMovementDate) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.status = status;
        this.budgetBasisAmount = budgetBasisAmount;
        this.committedCost = committedCost;
        this.actualCost = actualCost;
        this.committedVariance = committedVariance;
        this.actualVariance = actualVariance;
        this.budgetUtilizationPercent = budgetUtilizationPercent;
        this.totalIssuedQuantity = totalIssuedQuantity;
        this.totalReturnedQuantity = totalReturnedQuantity;
        this.netIssuedQuantity = netIssuedQuantity;
        this.totalIssueCount = totalIssueCount;
        this.totalReturnCount = totalReturnCount;
        this.totalReversalCount = totalReversalCount;
        this.movementCount = movementCount;
        this.lastMovementDate = lastMovementDate;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public BigDecimal getBudgetBasisAmount() {
        return budgetBasisAmount;
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

    public BigDecimal getBudgetUtilizationPercent() {
        return budgetUtilizationPercent;
    }

    public BigDecimal getTotalIssuedQuantity() {
        return totalIssuedQuantity;
    }

    public BigDecimal getTotalReturnedQuantity() {
        return totalReturnedQuantity;
    }

    public BigDecimal getNetIssuedQuantity() {
        return netIssuedQuantity;
    }

    public int getTotalIssueCount() {
        return totalIssueCount;
    }

    public int getTotalReturnCount() {
        return totalReturnCount;
    }

    public int getTotalReversalCount() {
        return totalReversalCount;
    }

    public int getMovementCount() {
        return movementCount;
    }

    public Instant getLastMovementDate() {
        return lastMovementDate;
    }
}
