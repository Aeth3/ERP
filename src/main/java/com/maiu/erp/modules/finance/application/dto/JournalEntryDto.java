package com.maiu.erp.modules.finance.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.enums.JournalEntryStatus;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;

public record JournalEntryDto(
        UUID id,
        String entryNumber,
        LocalDate entryDate,
        JournalSourceType sourceType,
        UUID sourceId,
        UUID stockMovementId,
        String memo,
        JournalEntryStatus status,
        UUID reversalOfId,
        UUID reversedById,
        BigDecimal totalAmount,
        String createdBy,
        Instant createdAt,
        List<JournalLineDto> lines) {
}
