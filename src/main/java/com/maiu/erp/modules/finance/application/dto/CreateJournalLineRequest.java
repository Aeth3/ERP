package com.maiu.erp.modules.finance.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateJournalLineRequest(
        @NotNull(message = "accountId is required")
        UUID accountId,
        BigDecimal debit,
        BigDecimal credit,
        UUID projectId,
        String memo) {
}
