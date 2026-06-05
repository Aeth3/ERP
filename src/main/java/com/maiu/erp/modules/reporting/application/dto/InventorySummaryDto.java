package com.maiu.erp.modules.reporting.application.dto;

import java.math.BigDecimal;

public class InventorySummaryDto {
    private final BigDecimal totalOnHand;
    private final BigDecimal totalReserved;
    private final BigDecimal totalAvailable;
    private final int stockRowCount;
    private final int productCount;
    private final int warehouseCount;
    private final int stockedWarehouseCount;

    public InventorySummaryDto(
            BigDecimal totalOnHand,
            BigDecimal totalReserved,
            BigDecimal totalAvailable,
            int stockRowCount,
            int productCount,
            int warehouseCount,
            int stockedWarehouseCount) {
        this.totalOnHand = totalOnHand;
        this.totalReserved = totalReserved;
        this.totalAvailable = totalAvailable;
        this.stockRowCount = stockRowCount;
        this.productCount = productCount;
        this.warehouseCount = warehouseCount;
        this.stockedWarehouseCount = stockedWarehouseCount;
    }

    public BigDecimal getTotalOnHand() {
        return totalOnHand;
    }

    public BigDecimal getTotalReserved() {
        return totalReserved;
    }

    public BigDecimal getTotalAvailable() {
        return totalAvailable;
    }

    public int getStockRowCount() {
        return stockRowCount;
    }

    public int getProductCount() {
        return productCount;
    }

    public int getWarehouseCount() {
        return warehouseCount;
    }

    public int getStockedWarehouseCount() {
        return stockedWarehouseCount;
    }
}
