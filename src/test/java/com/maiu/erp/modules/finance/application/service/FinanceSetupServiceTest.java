package com.maiu.erp.modules.finance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;
import com.maiu.erp.modules.finance.testsupport.InMemoryAccountMappingRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryAccountRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryFiscalPeriodRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryInventoryStockRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryJournalEntryRepository;

class FinanceSetupServiceTest {
    private InMemoryAccountRepository accountRepository;
    private InMemoryAccountMappingRepository mappingRepository;
    private FinanceSetupService service;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        mappingRepository = new InMemoryAccountMappingRepository();
        InMemoryJournalEntryRepository journalEntryRepository = new InMemoryJournalEntryRepository();
        LedgerPostingService ledgerPostingService = new LedgerPostingService(
                accountRepository,
                journalEntryRepository,
                new FiscalPeriodService(new InMemoryFiscalPeriodRepository()));
        service = new FinanceSetupService(
                accountRepository,
                mappingRepository,
                journalEntryRepository,
                new InMemoryInventoryStockRepository(),
                ledgerPostingService,
                "Asia/Manila");
    }

    @Test
    void ensureDefaults_seedsApAccountsAndMappings() {
        service.ensureDefaults();

        assertTrue(mappingRepository.findAccountId(AccountMappingKey.ACCOUNTS_PAYABLE).isPresent());
        assertTrue(mappingRepository.findAccountId(AccountMappingKey.PURCHASE_PRICE_VARIANCE).isPresent());
        assertTrue(mappingRepository.findAccountId(AccountMappingKey.CASH).isPresent());
        assertTrue(mappingRepository.findAccountId(AccountMappingKey.BANK).isPresent());

        assertEquals(
                accountRepository.findByCode("2000").orElseThrow().getId(),
                mappingRepository.findAccountId(AccountMappingKey.ACCOUNTS_PAYABLE).orElseThrow());
        assertEquals(
                accountRepository.findByCode("1000").orElseThrow().getId(),
                mappingRepository.findAccountId(AccountMappingKey.CASH).orElseThrow());
        assertEquals(
                accountRepository.findByCode("1010").orElseThrow().getId(),
                mappingRepository.findAccountId(AccountMappingKey.BANK).orElseThrow());
        assertEquals(
                accountRepository.findByCode("5150").orElseThrow().getId(),
                mappingRepository.findAccountId(AccountMappingKey.PURCHASE_PRICE_VARIANCE).orElseThrow());

        int accountCount = accountRepository.findAll().size();
        service.ensureDefaults();
        assertEquals(accountCount, accountRepository.findAll().size());
    }
}
