package com.maiu.erp.modules.finance.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.enums.FiscalPeriodStatus;

/** A calendar month; entries can only be posted into an OPEN period. */
public class FiscalPeriod {
    private UUID id;
    private int year;
    private int month;
    private LocalDate startDate;
    private LocalDate endDate;
    private FiscalPeriodStatus status;
    private String closedBy;
    private Instant closedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public FiscalPeriodStatus getStatus() {
        return status;
    }

    public void setStatus(FiscalPeriodStatus status) {
        this.status = status;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }
}
