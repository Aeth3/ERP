package com.maiu.erp.modules.finance.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Balances are signed on the account's normal side: positive means a normal balance. */
public record AccountLedgerDto(
        AccountDto account,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal openingBalance,
        List<AccountLedgerLineDto> lines,
        BigDecimal closingBalance) {
}
