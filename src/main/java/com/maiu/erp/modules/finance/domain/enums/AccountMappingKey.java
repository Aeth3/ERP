package com.maiu.erp.modules.finance.domain.enums;

/** Roles an account plays in automatic postings; each maps to one account in gl_account_mappings. */
public enum AccountMappingKey {
    INVENTORY,
    GOODS_RECEIVED_NOT_INVOICED,
    PROJECT_MATERIAL_COST,
    INVENTORY_ADJUSTMENT,
    COST_OF_SALES,
    OPENING_BALANCE_EQUITY,
    ACCOUNTS_PAYABLE,
    PURCHASE_PRICE_VARIANCE,
    CASH,
    BANK,
    ACCOUNTS_RECEIVABLE,
    RETENTION_RECEIVABLE,
    SALES_REVENUE,
    CONTRACT_REVENUE
}
