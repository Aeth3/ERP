package com.maiu.erp.modules.reporting.application.dto;

import java.time.LocalDate;
import java.util.List;

public class MovementSummaryDto {
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int totalMovementCount;
    private final List<MovementTypeCountDto> topMovementTypes;
    private final List<ReportMovementItemDto> recentMovements;

    public MovementSummaryDto(
            LocalDate startDate,
            LocalDate endDate,
            int totalMovementCount,
            List<MovementTypeCountDto> topMovementTypes,
            List<ReportMovementItemDto> recentMovements) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalMovementCount = totalMovementCount;
        this.topMovementTypes = topMovementTypes;
        this.recentMovements = recentMovements;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getTotalMovementCount() {
        return totalMovementCount;
    }

    public List<MovementTypeCountDto> getTopMovementTypes() {
        return topMovementTypes;
    }

    public List<ReportMovementItemDto> getRecentMovements() {
        return recentMovements;
    }
}
