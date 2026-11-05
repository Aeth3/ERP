package com.maiu.erp.modules.finance.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Inventory account balance vs total stock value; a non-zero difference needs investigating. */
public record InventoryReconciliationDto(
        BigDecimal ledgerBalance,
        BigDecimal stockValue,
        BigDecimal difference,
        List<WarehouseLine> warehouses) {

    public record WarehouseLine(UUID warehouseId, BigDecimal ledgerBalance, BigDecimal stockValue, BigDecimal difference) {
    }
}
