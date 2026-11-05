package com.maiu.erp.modules.finance.testsupport;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;
import com.maiu.erp.modules.finance.domain.repository.AccountMappingRepository;

public final class InMemoryAccountMappingRepository implements AccountMappingRepository {
    private final Map<AccountMappingKey, UUID> mappings = new EnumMap<>(AccountMappingKey.class);

    @Override
    public Optional<UUID> findAccountId(AccountMappingKey key) {
        return Optional.ofNullable(mappings.get(key));
    }

    @Override
    public Map<AccountMappingKey, UUID> findAll() {
        return Map.copyOf(mappings);
    }

    @Override
    public void save(AccountMappingKey key, UUID accountId) {
        mappings.put(key, accountId);
    }
}
