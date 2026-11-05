package com.maiu.erp.modules.finance.infrastructure.persistence.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.finance.domain.model.Account;
import com.maiu.erp.modules.finance.domain.repository.AccountRepository;
import com.maiu.erp.modules.finance.infrastructure.persistence.entity.AccountEntity;
import com.maiu.erp.modules.finance.infrastructure.persistence.jpa.AccountJpaRepository;

@Repository
public class AccountRepositoryImpl implements AccountRepository {
    private final AccountJpaRepository jpaRepository;

    public AccountRepositoryImpl(AccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Account save(Account account) {
        return toDomain(jpaRepository.save(toEntity(account)));
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Account> findByCode(String code) {
        return jpaRepository.findByCode(code).map(this::toDomain);
    }

    @Override
    public List<Account> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private Account toDomain(AccountEntity entity) {
        Account account = new Account();
        account.setId(entity.getId());
        account.setCode(entity.getCode());
        account.setName(entity.getName());
        account.setType(entity.getType());
        account.setActive(entity.isActive());
        account.setCreatedAt(entity.getCreatedAt());
        return account;
    }

    private AccountEntity toEntity(Account account) {
        AccountEntity entity = new AccountEntity();
        entity.setId(account.getId());
        entity.setCode(account.getCode());
        entity.setName(account.getName());
        entity.setType(account.getType());
        entity.setActive(account.isActive());
        entity.setCreatedAt(account.getCreatedAt());
        return entity;
    }
}
