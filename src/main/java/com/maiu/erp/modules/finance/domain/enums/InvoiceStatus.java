package com.maiu.erp.modules.finance.domain.enums;

/** Lifecycle of a customer invoice; CANCELLED is only reachable while nothing has been received. */
public enum InvoiceStatus {
    DRAFT,
    APPROVED,
    PARTIALLY_PAID,
    PAID,
    CANCELLED
}
