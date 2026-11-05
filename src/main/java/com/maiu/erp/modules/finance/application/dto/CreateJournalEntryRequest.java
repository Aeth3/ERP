package com.maiu.erp.modules.finance.application.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateJournalEntryRequest(
        @NotNull(message = "entryDate is required")
        LocalDate entryDate,
        String memo,
        @NotEmpty(message = "lines are required")
        List<@Valid CreateJournalLineRequest> lines) {
}
