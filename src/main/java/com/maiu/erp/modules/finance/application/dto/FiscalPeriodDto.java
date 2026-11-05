package com.maiu.erp.modules.finance.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.enums.FiscalPeriodStatus;

public record FiscalPeriodDto(
        UUID id,
        int year,
        int month,
        LocalDate startDate,
        LocalDate endDate,
        FiscalPeriodStatus status,
        String closedBy,
        Instant closedAt) {
}
