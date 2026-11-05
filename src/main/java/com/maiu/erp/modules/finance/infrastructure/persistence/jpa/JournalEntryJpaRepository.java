package com.maiu.erp.modules.finance.infrastructure.persistence.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.finance.infrastructure.persistence.entity.JournalEntryEntity;

public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryEntity, UUID> {
    boolean existsByEntryNumber(String entryNumber);

    boolean existsByStockMovementId(UUID stockMovementId);

    List<JournalEntryEntity> findByEntryDateBetween(LocalDate startDate, LocalDate endDate);

    List<JournalEntryEntity> findBySourceId(UUID sourceId);
}
