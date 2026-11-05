package com.maiu.erp.modules.finance.infrastructure.persistence.impl;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;
import com.maiu.erp.modules.finance.domain.repository.AccountMappingRepository;
import com.maiu.erp.modules.finance.infrastructure.persistence.entity.AccountMappingEntity;
import com.maiu.erp.modules.finance.infrastructure.persistence.jpa.AccountMappingJpaRepository;

@Repository
public class AccountMappingRepositoryImpl implements AccountMappingRepository {
    private final AccountMappingJpaRepository jpaRepository;

    public AccountMappingRepositoryImpl(AccountMappingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<UUID> findAccountId(AccountMappingKey key) {
        return jpaRepository.findById(key.name()).map(AccountMappingEntity::getAccountId);
    }

    @Override
    public Map<AccountMappingKey, UUID> findAll() {
        Map<AccountMappingKey, UUID> mappings = new EnumMap<>(AccountMappingKey.class);
        for (AccountMappingEntity entity : jpaRepository.findAll()) {
            try {
                mappings.put(AccountMappingKey.valueOf(entity.getMappingKey()), entity.getAccountId());
            } catch (IllegalArgumentException ignored) {
                // A key no longer used by the code; leave it in the table untouched.
            }
        }
        return mappings;
    }

    @Override
    public void save(AccountMappingKey key, UUID accountId) {
        AccountMappingEntity entity = new AccountMappingEntity();
        entity.setMappingKey(key.name());
        entity.setAccountId(accountId);
        jpaRepository.save(entity);
    }
}
