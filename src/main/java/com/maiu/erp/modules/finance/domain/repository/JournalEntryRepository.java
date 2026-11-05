package com.maiu.erp.modules.finance.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.model.JournalEntry;

public interface JournalEntryRepository {
    JournalEntry save(JournalEntry entry);

    Optional<JournalEntry> findById(UUID id);

    boolean existsByEntryNumber(String entryNumber);

    boolean existsByStockMovementId(UUID stockMovementId);

    boolean existsAny();

    List<JournalEntry> findByDateRange(LocalDate startDate, LocalDate endDate);

    List<JournalEntry> findBySourceId(UUID sourceId);
}
