package com.maiu.erp.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateMaterialReturnItemRequest(
        @NotNull(message = "productId is required")
        UUID productId,
        @NotNull(message = "quantity is required")
        BigDecimal quantity) {
}
