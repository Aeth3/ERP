package com.maiu.erp.modules.finance.domain.repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;

public interface AccountMappingRepository {
    Optional<UUID> findAccountId(AccountMappingKey key);

    Map<AccountMappingKey, UUID> findAll();

    void save(AccountMappingKey key, UUID accountId);
}
