package com.maiu.erp.modules.reporting.application.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProjectSummaryDto {
    private final int totalProjectCount;
    private final int activeProjectCount;
    private final int overBudgetProjectCount;
    private final BigDecimal totalCommittedCost;
    private final BigDecimal totalActualCost;
    private final List<ProjectRiskItemDto> budgetRiskProjects;

    public ProjectSummaryDto(
            int totalProjectCount,
            int activeProjectCount,
            int overBudgetProjectCount,
            BigDecimal totalCommittedCost,
            BigDecimal totalActualCost,
            List<ProjectRiskItemDto> budgetRiskProjects) {
        this.totalProjectCount = totalProjectCount;
        this.activeProjectCount = activeProjectCount;
        this.overBudgetProjectCount = overBudgetProjectCount;
        this.totalCommittedCost = totalCommittedCost;
        this.totalActualCost = totalActualCost;
        this.budgetRiskProjects = budgetRiskProjects;
    }

    public int getTotalProjectCount() {
        return totalProjectCount;
    }

    public int getActiveProjectCount() {
        return activeProjectCount;
    }

    public int getOverBudgetProjectCount() {
        return overBudgetProjectCount;
    }

    public BigDecimal getTotalCommittedCost() {
        return totalCommittedCost;
    }

    public BigDecimal getTotalActualCost() {
        return totalActualCost;
    }

    public List<ProjectRiskItemDto> getBudgetRiskProjects() {
        return budgetRiskProjects;
    }
}
