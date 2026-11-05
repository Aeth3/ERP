package com.maiu.erp.modules.finance.application.dto;

import java.time.LocalDate;

public record ReverseJournalEntryRequest(LocalDate reversalDate, String memo) {
}
