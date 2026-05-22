package com.maiu.erp.modules.inventory.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUnitRequest(
        @NotBlank(message = "name is required")
        String name,
        String symbol,
        Boolean active) {
}
