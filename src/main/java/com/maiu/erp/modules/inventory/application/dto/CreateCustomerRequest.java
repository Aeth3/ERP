package com.maiu.erp.modules.inventory.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(
        @NotBlank(message = "code is required")
        String code,
        @NotBlank(message = "name is required")
        String name,
        String contactPerson,
        String phone,
        String email,
        String address,
        Boolean active) {
}
