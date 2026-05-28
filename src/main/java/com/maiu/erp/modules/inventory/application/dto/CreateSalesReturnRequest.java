package com.maiu.erp.modules.inventory.application.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateSalesReturnRequest(
        String remarks,
        @NotBlank(message = "performedBy is required")
        String performedBy,
        @NotEmpty(message = "items are required")
        List<@Valid CreateSalesReturnItemRequest> items) {
}
