package com.maiu.erp.modules.project.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProjectBudgetLineRequest(
        @NotBlank(message = "costCode is required")
        String costCode,
        @NotBlank(message = "description is required")
        String description,
        @NotNull(message = "budgetAmount is required")
        BigDecimal budgetAmount) {
}
