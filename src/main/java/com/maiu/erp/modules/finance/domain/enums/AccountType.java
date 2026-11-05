package com.maiu.erp.modules.finance.domain.enums;

public enum AccountType {
    ASSET,
    LIABILITY,
    EQUITY,
    REVENUE,
    EXPENSE;

    /** Assets and expenses grow with debits; the rest grow with credits. */
    public boolean isDebitNormal() {
        return this == ASSET || this == EXPENSE;
    }
}
