package com.maiu.erp.modules.finance.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateAccountMappingRequest(
        @NotNull(message = "accountId is required")
        UUID accountId) {
}
