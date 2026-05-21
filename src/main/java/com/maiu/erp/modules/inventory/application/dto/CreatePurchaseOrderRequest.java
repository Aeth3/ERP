package com.maiu.erp.modules.inventory.application.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreatePurchaseOrderRequest(
        @NotNull(message = "supplierId is required")
        UUID supplierId,
        @NotNull(message = "orderDate is required")
        LocalDate orderDate,
        LocalDate expectedDate,
        @NotEmpty(message = "items are required")
        List<@Valid CreatePurchaseOrderItemRequest> items) {
}
