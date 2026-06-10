package com.maiu.erp.modules.reporting.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class StockMovementReportSummaryDto {
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int totalMovements;
    private final BigDecimal totalQuantity;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final List<ReportMovementItemDto> movements;

    public StockMovementReportSummaryDto(
            LocalDate startDate,
            LocalDate endDate,
            int totalMovements,
            BigDecimal totalQuantity,
            int page,
            int size,
            long totalElements,
            int totalPages,
            List<ReportMovementItemDto> movements) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalMovements = totalMovements;
        this.totalQuantity = totalQuantity;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.movements = movements;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getTotalMovements() {
        return totalMovements;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public List<ReportMovementItemDto> getMovements() {
        return movements;
    }
}
