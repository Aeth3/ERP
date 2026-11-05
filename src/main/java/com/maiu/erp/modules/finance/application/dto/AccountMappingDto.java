package com.maiu.erp.modules.finance.application.dto;

import java.util.UUID;

import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;

public record AccountMappingDto(AccountMappingKey key, UUID accountId, String accountCode, String accountName) {
}
