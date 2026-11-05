package com.maiu.erp.modules.finance.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record JournalLineDto(
        UUID id,
        int lineNumber,
        UUID accountId,
        String accountCode,
        String accountName,
        BigDecimal debit,
        BigDecimal credit,
        UUID projectId,
        UUID warehouseId,
        UUID productId,
        String memo) {
}
