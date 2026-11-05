package com.maiu.erp.modules.finance.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.enums.AccountType;

/** periodDebit/periodCredit cover the date range; debitBalance/creditBalance are the closing balance at endDate. */
public record TrialBalanceRowDto(
        UUID accountId,
        String code,
        String name,
        AccountType type,
        BigDecimal periodDebit,
        BigDecimal periodCredit,
        BigDecimal debitBalance,
        BigDecimal creditBalance) {
}
