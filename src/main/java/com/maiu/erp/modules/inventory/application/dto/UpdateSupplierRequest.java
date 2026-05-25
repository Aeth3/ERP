package com.maiu.erp.modules.inventory.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateSupplierRequest(
        @NotBlank(message = "code is required")
        String code,
        @NotBlank(message = "name is required")
        String name,
        String contactPerson,
        String phone,
        @Email(message = "email must be valid")
        String email,
        String address,
        Boolean active) {
}
