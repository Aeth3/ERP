package com.maiu.erp.modules.reporting.application.dto;

import java.util.List;

public class PurchaseOrderReportDetailDto {
    private final PurchaseOrderReportItemDto summary;
    private final List<ReportLineItemDto> items;
    private final List<ReportReturnDto> returns;

    public PurchaseOrderReportDetailDto(
            PurchaseOrderReportItemDto summary,
            List<ReportLineItemDto> items,
            List<ReportReturnDto> returns) {
        this.summary = summary;
        this.items = items;
        this.returns = returns;
    }

    public PurchaseOrderReportItemDto getSummary() {
        return summary;
    }

    public List<ReportLineItemDto> getItems() {
        return items;
    }

    public List<ReportReturnDto> getReturns() {
        return returns;
    }
}
