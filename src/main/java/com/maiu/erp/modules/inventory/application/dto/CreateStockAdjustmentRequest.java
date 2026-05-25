package com.maiu.erp.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStockAdjustmentRequest(
        @NotNull(message = "productId is required")
        UUID productId,
        @NotNull(message = "warehouseId is required")
        UUID warehouseId,
        @NotBlank(message = "adjustmentType is required")
        String adjustmentType,
        @NotNull(message = "quantity is required")
        BigDecimal quantity,
        BigDecimal unitCost,
        @NotBlank(message = "reason is required")
        String reason,
        String notes,
        @NotBlank(message = "performedBy is required")
        String performedBy) {
}
