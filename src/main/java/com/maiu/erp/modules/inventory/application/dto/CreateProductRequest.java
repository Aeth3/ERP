package com.maiu.erp.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateProductRequest(
        @NotBlank(message = "sku is required")
        String sku,
        @NotBlank(message = "name is required")
        String name,
        String description,
        @NotNull(message = "costPrice is required")
        @PositiveOrZero(message = "costPrice must be zero or greater")
        BigDecimal costPrice,
        @NotNull(message = "sellingPrice is required")
        @PositiveOrZero(message = "sellingPrice must be zero or greater")
        BigDecimal sellingPrice,
        UUID categoryId,
        UUID unitId) {
}
