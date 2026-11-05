package com.maiu.erp.modules.finance.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TrialBalanceDto(
        LocalDate startDate,
        LocalDate endDate,
        List<TrialBalanceRowDto> rows,
        BigDecimal totalPeriodDebit,
        BigDecimal totalPeriodCredit,
        BigDecimal totalDebitBalance,
        BigDecimal totalCreditBalance,
        boolean balanced) {
}
