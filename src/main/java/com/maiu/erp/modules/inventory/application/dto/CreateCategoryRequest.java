package com.maiu.erp.modules.inventory.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank(message = "name is required")
        String name,
        UUID parentId,
        Boolean active) {
}
