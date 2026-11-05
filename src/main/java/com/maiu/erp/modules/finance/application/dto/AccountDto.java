package com.maiu.erp.modules.finance.application.dto;

import java.util.UUID;

import com.maiu.erp.modules.finance.domain.enums.AccountType;

public record AccountDto(UUID id, String code, String name, AccountType type, boolean active) {
}
