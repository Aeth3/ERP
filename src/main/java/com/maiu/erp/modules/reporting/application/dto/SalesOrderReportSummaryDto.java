package com.maiu.erp.modules.reporting.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class SalesOrderReportSummaryDto {
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Integer totalOrders;
    private final BigDecimal totalAmount;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final List<SalesOrderReportItemDto> orders;

    public SalesOrderReportSummaryDto(
            LocalDate startDate,
            LocalDate endDate,
            Integer totalOrders,
            BigDecimal totalAmount,
            int page,
            int size,
            long totalElements,
            int totalPages,
            List<SalesOrderReportItemDto> orders) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalOrders = totalOrders;
        this.totalAmount = totalAmount;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.orders = orders;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
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

    public List<SalesOrderReportItemDto> getOrders() {
        return orders;
    }
}
