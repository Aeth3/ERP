package com.maiu.erp.modules.inventory.application.dto;

import jakarta.validation.constraints.NotNull;

public record ShipSalesOrderRequest(
        @NotNull(message = "warehouseId is required")
        java.util.UUID warehouseId,
        @NotNull(message = "performedBy is required")
        Long performedBy) {
}
