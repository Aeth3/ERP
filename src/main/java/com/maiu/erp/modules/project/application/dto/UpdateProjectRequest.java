package com.maiu.erp.modules.project.application.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateProjectRequest(
        @NotBlank(message = "projectName is required")
        String projectName,
        @NotNull(message = "customerId is required")
        UUID customerId,
        String location,
        LocalDate startDate,
        LocalDate targetEndDate) {
}
