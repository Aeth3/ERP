package com.maiu.erp.modules.inventory.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ConfirmSalesOrderRequest(
        @NotNull(message = "warehouseId is required")
        UUID warehouseId) {
}
