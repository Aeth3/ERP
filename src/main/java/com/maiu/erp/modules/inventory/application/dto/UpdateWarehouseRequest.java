package com.maiu.erp.modules.inventory.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateWarehouseRequest(
        @NotBlank(message = "code is required")
        String code,
        @NotBlank(message = "name is required")
        String name,
        String address,
        Boolean active) {
}
