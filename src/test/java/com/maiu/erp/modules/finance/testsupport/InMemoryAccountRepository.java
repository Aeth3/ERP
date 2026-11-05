package com.maiu.erp.modules.finance.testsupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.model.Account;
import com.maiu.erp.modules.finance.domain.repository.AccountRepository;

public final class InMemoryAccountRepository implements AccountRepository {
    private final Map<UUID, Account> accounts = new HashMap<>();

    @Override
    public Account save(Account account) {
        if (account.getId() == null) {
            account.setId(UUID.randomUUID());
        }
        accounts.put(account.getId(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public Optional<Account> findByCode(String code) {
        return accounts.values().stream().filter(account -> code.equals(account.getCode())).findFirst();
    }

    @Override
    public List<Account> findAll() {
        return List.copyOf(accounts.values());
    }
}
