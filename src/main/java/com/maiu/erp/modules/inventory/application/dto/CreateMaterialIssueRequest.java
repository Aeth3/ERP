package com.maiu.erp.modules.inventory.application.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateMaterialIssueRequest(
        @NotNull(message = "projectId is required")
        UUID projectId,
        @NotNull(message = "warehouseId is required")
        UUID warehouseId,
        String remarks,
        @NotBlank(message = "performedBy is required")
        String performedBy,
        @NotEmpty(message = "items are required")
        List<@Valid CreateMaterialIssueItemRequest> items) {
}
