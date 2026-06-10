package com.maiu.erp.modules.reporting.application.dto;

import java.math.BigDecimal;

public class StockMovementReportDetailDto {
    private final ReportMovementItemDto summary;
    private final BigDecimal totalValue;

    public StockMovementReportDetailDto(ReportMovementItemDto summary, BigDecimal totalValue) {
        this.summary = summary;
        this.totalValue = totalValue;
    }

    public ReportMovementItemDto getSummary() {
        return summary;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }
}
