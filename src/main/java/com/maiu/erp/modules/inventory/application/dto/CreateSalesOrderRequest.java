package com.maiu.erp.modules.inventory.application.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateSalesOrderRequest(
        @NotNull(message = "customerId is required")
        UUID customerId,
        @NotNull(message = "orderDate is required")
        LocalDate orderDate,
        @NotEmpty(message = "items are required")
        List<@Valid CreateSalesOrderItemRequest> items) {
}
