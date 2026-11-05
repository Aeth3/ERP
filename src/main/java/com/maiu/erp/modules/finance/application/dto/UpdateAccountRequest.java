package com.maiu.erp.modules.finance.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountRequest(
        @NotBlank(message = "name is required")
        String name,
        @NotNull(message = "active is required")
        Boolean active) {
}
