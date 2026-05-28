package com.maiu.erp.modules.inventory.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ReverseMaterialIssueRequest(
        String remarks,
        @NotBlank(message = "performedBy is required")
        String performedBy) {
}
