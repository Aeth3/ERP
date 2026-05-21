package com.maiu.erp.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateSalesOrderItemRequest(
        @NotNull(message = "productId is required")
        UUID productId,
        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than zero")
        BigDecimal quantity,
        @NotNull(message = "unitPrice is required")
        @PositiveOrZero(message = "unitPrice must be zero or greater")
        BigDecimal unitPrice) {
}
