package com.maiu.erp.modules.reporting.application.dto;

import java.util.List;

public class SalesOrderReportDetailDto {
    private final SalesOrderReportItemDto summary;
    private final List<ReportLineItemDto> items;
    private final List<ReportReturnDto> returns;

    public SalesOrderReportDetailDto(
            SalesOrderReportItemDto summary,
            List<ReportLineItemDto> items,
            List<ReportReturnDto> returns) {
        this.summary = summary;
        this.items = items;
        this.returns = returns;
    }

    public SalesOrderReportItemDto getSummary() {
        return summary;
    }

    public List<ReportLineItemDto> getItems() {
        return items;
    }

    public List<ReportReturnDto> getReturns() {
        return returns;
    }
}
