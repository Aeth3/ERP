package com.maiu.erp.modules.finance.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.enums.JournalEntryStatus;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "gl_journal_entries")
public class JournalEntryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String entryNumber;

    @Column(nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false)
    private UUID fiscalPeriodId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JournalSourceType sourceType;

    private UUID sourceId;

    // One automatic entry per stock movement: a movement can never be posted twice.
    @Column(unique = true)
    private UUID stockMovementId;

    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JournalEntryStatus status;

    private UUID reversalOfId;

    private UUID reversedById;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    @OrderBy("lineNumber ASC")
    private List<JournalLineEntity> lines = new ArrayList<>();

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

    public List<JournalLineEntity> getLines() {
        return lines;
    }

    public void setLines(List<JournalLineEntity> lines) {
        this.lines = lines;
    }
}
