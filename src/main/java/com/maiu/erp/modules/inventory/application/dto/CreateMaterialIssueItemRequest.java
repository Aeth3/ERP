package com.maiu.erp.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateMaterialIssueItemRequest(
        @NotNull(message = "productId is required")
        UUID productId,
        @NotNull(message = "quantity is required")
        @DecimalMin(value = "0.0000001", message = "quantity must be greater than zero")
        BigDecimal quantity,
        // Optional: used only when the warehouse has no average cost for the product yet.
        @DecimalMin(value = "0.0", inclusive = true, message = "unitCost must be zero or greater")
        BigDecimal unitCost) {
}
