package com.maiu.erp.modules.inventory.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductCommand(
        String sku,
        String name,
        String description,
        BigDecimal costPrice,
        BigDecimal sellingPrice,
        UUID categoryId,
        UUID unitId) {
}