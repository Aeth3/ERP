package com.maiu.erp.modules.finance.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;

public record AccountLedgerLineDto(
        UUID entryId,
        String entryNumber,
        LocalDate entryDate,
        JournalSourceType sourceType,
        String memo,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal runningBalance,
        UUID projectId,
        UUID warehouseId) {
}
