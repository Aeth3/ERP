package com.maiu.erp.modules.finance.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.enums.JournalEntryStatus;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;

/** A posted, balanced set of lines. Posted entries are never edited; they are reversed. */
public class JournalEntry {
    private UUID id;
    private String entryNumber;
    private LocalDate entryDate;
    private UUID fiscalPeriodId;
    private JournalSourceType sourceType;
    private UUID sourceId;
    private UUID stockMovementId;
    private String memo;
    private JournalEntryStatus status;
    private UUID reversalOfId;
    private UUID reversedById;
    private BigDecimal totalAmount;
    private String createdBy;
    private Instant createdAt;
    private List<JournalLine> lines = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEntryNumber() {
        return entryNumber;
    }

    public void setEntryNumber(String entryNumber) {
        this.entryNumber = entryNumber;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public UUID getFiscalPeriodId() {
        return fiscalPeriodId;
    }

    public void setFiscalPeriodId(UUID fiscalPeriodId) {
        this.fiscalPeriodId = fiscalPeriodId;
    }

    public JournalSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(JournalSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public UUID getStockMovementId() {
        return stockMovementId;
    }

    public void setStockMovementId(UUID stockMovementId) {
        this.stockMovementId = stockMovementId;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public JournalEntryStatus getStatus() {
        return status;
    }

    public void setStatus(JournalEntryStatus status) {
        this.status = status;
    }

    public UUID getReversalOfId() {
        return reversalOfId;
    }

    public void setReversalOfId(UUID reversalOfId) {
        this.reversalOfId = reversalOfId;
    }

    public UUID getReversedById() {
        return reversedById;
    }

    public void setReversedById(UUID reversedById) {
        this.reversedById = reversedById;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<JournalLine> getLines() {
        return lines;
    }

    public void setLines(List<JournalLine> lines) {
        this.lines = lines;
    }
}
