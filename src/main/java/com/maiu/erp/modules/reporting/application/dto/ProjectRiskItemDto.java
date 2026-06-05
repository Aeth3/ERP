package com.maiu.erp.modules.reporting.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.maiu.erp.modules.project.domain.enums.ProjectStatus;

public class ProjectRiskItemDto {
    private final UUID projectId;
    private final String projectCode;
    private final String projectName;
    private final ProjectStatus status;
    private final BigDecimal budgetBasisAmount;
    private final BigDecimal committedCost;
    private final BigDecimal actualCost;
    private final BigDecimal actualVariance;

    public ProjectRiskItemDto(
            UUID projectId,
            String projectCode,
            String projectName,
            ProjectStatus status,
            BigDecimal budgetBasisAmount,
            BigDecimal committedCost,
            BigDecimal actualCost,
            BigDecimal actualVariance) {
        this.projectId = projectId;
        this.projectCode = projectCode;
        this.projectName = projectName;
        this.status = status;
        this.budgetBasisAmount = budgetBasisAmount;
        this.committedCost = committedCost;
        this.actualCost = actualCost;
        this.actualVariance = actualVariance;
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

    public BigDecimal getActualVariance() {
        return actualVariance;
    }
}
