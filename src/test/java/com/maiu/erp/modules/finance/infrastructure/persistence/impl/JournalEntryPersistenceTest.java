package com.maiu.erp.modules.finance.infrastructure.persistence.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.maiu.erp.modules.finance.application.service.FiscalPeriodService;
import com.maiu.erp.modules.finance.application.service.LedgerPostingService;
import com.maiu.erp.modules.finance.domain.enums.AccountType;
import com.maiu.erp.modules.finance.domain.enums.JournalEntryStatus;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;
import com.maiu.erp.modules.finance.domain.model.Account;
import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.model.JournalLine;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({
        AccountRepositoryImpl.class,
        JournalEntryRepositoryImpl.class,
        FiscalPeriodRepositoryImpl.class,
        FiscalPeriodService.class,
        LedgerPostingService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JournalEntryPersistenceTest {

    @Autowired
    private AccountRepositoryImpl accountRepository;

    @Autowired
    private JournalEntryRepositoryImpl journalEntryRepository;

    @Autowired
    private LedgerPostingService ledgerPostingService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void postedEntryRoundTripsWithOrderedLinesAndReversalUpdatesTheOriginal() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Account inventory = transaction.execute(status -> account("1300", AccountType.ASSET));
        Account equity = transaction.execute(status -> account("3900", AccountType.EQUITY));
        UUID projectId = UUID.randomUUID();

        UUID entryId = transaction.execute(status -> {
            JournalEntry entry = new JournalEntry();
            entry.setEntryDate(LocalDate.now());
            entry.setSourceType(JournalSourceType.OPENING_BALANCE);
            entry.setCreatedBy("system");
            entry.setLines(List.of(
                    line(inventory, "250.00", null, projectId),
                    line(equity, null, "250.00", null)));
            return ledgerPostingService.post(entry).getId();
        });

        JournalEntry reloaded = journalEntryRepository.findById(entryId).orElseThrow();
        assertEquals(2, reloaded.getLines().size());
        assertEquals(1, reloaded.getLines().get(0).getLineNumber());
        assertEquals(inventory.getId(), reloaded.getLines().get(0).getAccountId());
        assertEquals(new BigDecimal("250.00"), reloaded.getLines().get(0).getDebit());
        assertEquals(projectId, reloaded.getLines().get(0).getProjectId());
        assertEquals(new BigDecimal("250.00"), reloaded.getTotalAmount());

        UUID reversalId = transaction.execute(status ->
                ledgerPostingService.reverse(entryId, LocalDate.now(), null, "controller").getId());

        JournalEntry original = journalEntryRepository.findById(entryId).orElseThrow();
        assertEquals(JournalEntryStatus.REVERSED, original.getStatus());
        assertEquals(reversalId, original.getReversedById());
        assertEquals(2, original.getLines().size());
        JournalEntry reversal = journalEntryRepository.findById(reversalId).orElseThrow();
        assertEquals(new BigDecimal("250.00"), reversal.getLines().get(0).getCredit());
    }

    private Account account(String code, AccountType type) {
        Account account = new Account();
        account.setCode(code);
        account.setName("Account " + code);
        account.setType(type);
        account.setActive(true);
        account.setCreatedAt(Instant.now());
        return accountRepository.save(account);
    }

    private JournalLine line(Account account, String debit, String credit, UUID projectId) {
        JournalLine line = new JournalLine();
        line.setAccountId(account.getId());
        line.setDebit(debit == null ? null : new BigDecimal(debit));
        line.setCredit(credit == null ? null : new BigDecimal(credit));
        line.setProjectId(projectId);
        return line;
    }
}
