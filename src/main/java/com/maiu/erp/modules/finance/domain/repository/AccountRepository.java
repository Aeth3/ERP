package com.maiu.erp.modules.finance.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.model.Account;

public interface AccountRepository {
    Account save(Account account);

    Optional<Account> findById(UUID id);

    Optional<Account> findByCode(String code);

    List<Account> findAll();
}
