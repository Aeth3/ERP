package com.maiu.erp.modules.reporting.application.dto;

public class ReportingOverviewDto {
    private final InventorySummaryDto inventory;
    private final MovementSummaryDto movement;
    private final ProjectSummaryDto project;

    public ReportingOverviewDto(
            InventorySummaryDto inventory,
            MovementSummaryDto movement,
            ProjectSummaryDto project) {
        this.inventory = inventory;
        this.movement = movement;
        this.project = project;
    }

    public InventorySummaryDto getInventory() {
        return inventory;
    }

    public MovementSummaryDto getMovement() {
        return movement;
    }

    public ProjectSummaryDto getProject() {
        return project;
    }
}
