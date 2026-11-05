package com.maiu.erp.modules.finance.testsupport;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.repository.JournalEntryRepository;

public final class InMemoryJournalEntryRepository implements JournalEntryRepository {
    private final List<JournalEntry> entries = new ArrayList<>();

    @Override
    public JournalEntry save(JournalEntry entry) {
        if (entry.getId() == null) {
            entry.setId(UUID.randomUUID());
            entries.add(entry);
        }
        return entry;
    }

    @Override
    public Optional<JournalEntry> findById(UUID id) {
        return entries.stream().filter(entry -> id.equals(entry.getId())).findFirst();
    }

    @Override
    public boolean existsByEntryNumber(String entryNumber) {
        return entries.stream().anyMatch(entry -> entryNumber.equals(entry.getEntryNumber()));
    }

    @Override
    public boolean existsByStockMovementId(UUID stockMovementId) {
        return entries.stream().anyMatch(entry -> stockMovementId.equals(entry.getStockMovementId()));
    }

    @Override
    public boolean existsAny() {
        return !entries.isEmpty();
    }

    @Override
    public List<JournalEntry> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return entries.stream()
                .filter(entry -> !entry.getEntryDate().isBefore(startDate) && !entry.getEntryDate().isAfter(endDate))
                .toList();
    }

    @Override
    public List<JournalEntry> findBySourceId(UUID sourceId) {
        return entries.stream().filter(entry -> sourceId.equals(entry.getSourceId())).toList();
    }
}
