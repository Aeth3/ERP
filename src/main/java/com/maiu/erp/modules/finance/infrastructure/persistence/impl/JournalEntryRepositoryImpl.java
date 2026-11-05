package com.maiu.erp.modules.finance.infrastructure.persistence.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.model.JournalLine;
import com.maiu.erp.modules.finance.domain.repository.JournalEntryRepository;
import com.maiu.erp.modules.finance.infrastructure.persistence.entity.JournalEntryEntity;
import com.maiu.erp.modules.finance.infrastructure.persistence.entity.JournalLineEntity;
import com.maiu.erp.modules.finance.infrastructure.persistence.jpa.JournalEntryJpaRepository;

@Repository
public class JournalEntryRepositoryImpl implements JournalEntryRepository {
    private final JournalEntryJpaRepository jpaRepository;

    public JournalEntryRepositoryImpl(JournalEntryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public JournalEntry save(JournalEntry entry) {
        return toDomain(jpaRepository.save(toEntity(entry)));
    }

    @Override
    public Optional<JournalEntry> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByEntryNumber(String entryNumber) {
        return jpaRepository.existsByEntryNumber(entryNumber);
    }

    @Override
    public boolean existsByStockMovementId(UUID stockMovementId) {
        return jpaRepository.existsByStockMovementId(stockMovementId);
    }

    @Override
    public boolean existsAny() {
        return jpaRepository.count() > 0;
    }

    @Override
    public List<JournalEntry> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByEntryDateBetween(startDate, endDate).stream().map(this::toDomain).toList();
    }

    @Override
    public List<JournalEntry> findBySourceId(UUID sourceId) {
        return jpaRepository.findBySourceId(sourceId).stream().map(this::toDomain).toList();
    }

    private JournalEntry toDomain(JournalEntryEntity entity) {
        JournalEntry entry = new JournalEntry();
        entry.setId(entity.getId());
        entry.setEntryNumber(entity.getEntryNumber());
        entry.setEntryDate(entity.getEntryDate());
        entry.setFiscalPeriodId(entity.getFiscalPeriodId());
        entry.setSourceType(entity.getSourceType());
        entry.setSourceId(entity.getSourceId());
        entry.setStockMovementId(entity.getStockMovementId());
        entry.setMemo(entity.getMemo());
        entry.setStatus(entity.getStatus());
        entry.setReversalOfId(entity.getReversalOfId());
        entry.setReversedById(entity.getReversedById());
        entry.setTotalAmount(entity.getTotalAmount());
        entry.setCreatedBy(entity.getCreatedBy());
        entry.setCreatedAt(entity.getCreatedAt());
        List<JournalLine> lines = new ArrayList<>();
        for (JournalLineEntity lineEntity : entity.getLines()) {
            JournalLine line = new JournalLine();
            line.setId(lineEntity.getId());
            line.setLineNumber(lineEntity.getLineNumber());
            line.setAccountId(lineEntity.getAccountId());
            line.setDebit(lineEntity.getDebit());
            line.setCredit(lineEntity.getCredit());
            line.setProjectId(lineEntity.getProjectId());
            line.setWarehouseId(lineEntity.getWarehouseId());
            line.setProductId(lineEntity.getProductId());
            line.setMemo(lineEntity.getMemo());
            lines.add(line);
        }
        entry.setLines(lines);
        return entry;
    }

    private JournalEntryEntity toEntity(JournalEntry entry) {
        JournalEntryEntity entity = new JournalEntryEntity();
        entity.setId(entry.getId());
        entity.setEntryNumber(entry.getEntryNumber());
        entity.setEntryDate(entry.getEntryDate());
        entity.setFiscalPeriodId(entry.getFiscalPeriodId());
        entity.setSourceType(entry.getSourceType());
        entity.setSourceId(entry.getSourceId());
        entity.setStockMovementId(entry.getStockMovementId());
        entity.setMemo(entry.getMemo());
        entity.setStatus(entry.getStatus());
        entity.setReversalOfId(entry.getReversalOfId());
        entity.setReversedById(entry.getReversedById());
        entity.setTotalAmount(entry.getTotalAmount());
        entity.setCreatedBy(entry.getCreatedBy());
        entity.setCreatedAt(entry.getCreatedAt());
        List<JournalLineEntity> lineEntities = new ArrayList<>();
        for (JournalLine line : entry.getLines()) {
            JournalLineEntity lineEntity = new JournalLineEntity();
            lineEntity.setId(line.getId());
            lineEntity.setLineNumber(line.getLineNumber());
            lineEntity.setAccountId(line.getAccountId());
            lineEntity.setDebit(line.getDebit());
            lineEntity.setCredit(line.getCredit());
            lineEntity.setProjectId(line.getProjectId());
            lineEntity.setWarehouseId(line.getWarehouseId());
            lineEntity.setProductId(line.getProductId());
            lineEntity.setMemo(line.getMemo());
            lineEntities.add(lineEntity);
        }
        entity.setLines(lineEntities);
        return entity;
    }
}
