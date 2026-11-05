package com.maiu.erp.modules.finance.application.dto;

import com.maiu.erp.modules.finance.domain.enums.AccountType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank(message = "code is required")
        @Size(max = 20, message = "code must be at most 20 characters")
        String code,
        @NotBlank(message = "name is required")
        String name,
        @NotNull(message = "type is required")
        AccountType type) {
}
