package com.maiu.erp.modules.project.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ProjectBudgetLineDto {
    private final UUID id;
    private final UUID projectId;
    private final String costCode;
    private final String description;
    private final BigDecimal budgetAmount;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ProjectBudgetLineDto(
            UUID id,
            UUID projectId,
            String costCode,
            String description,
            BigDecimal budgetAmount,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.costCode = costCode;
        this.description = description;
        this.budgetAmount = budgetAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getCostCode() {
        return costCode;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
