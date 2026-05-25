package com.maiu.erp.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStockTransferRequest(
        @NotNull(message = "productId is required")
        UUID productId,
        @NotNull(message = "fromWarehouseId is required")
        UUID fromWarehouseId,
        @NotNull(message = "toWarehouseId is required")
        UUID toWarehouseId,
        @NotNull(message = "quantity is required")
        BigDecimal quantity,
        BigDecimal unitCost,
        @NotBlank(message = "reason is required")
        String reason,
        String notes,
        @NotBlank(message = "performedBy is required")
        String performedBy) {
}
