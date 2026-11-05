package com.maiu.erp.modules.finance.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.finance.application.dto.CreateAccountRequest;
import com.maiu.erp.modules.finance.application.dto.CreateJournalEntryRequest;
import com.maiu.erp.modules.finance.application.dto.CreateJournalLineRequest;
import com.maiu.erp.modules.finance.application.dto.ReverseJournalEntryRequest;
import com.maiu.erp.modules.finance.application.dto.UpdateAccountRequest;
import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;
import com.maiu.erp.modules.finance.domain.model.Account;
import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.model.JournalLine;
import com.maiu.erp.modules.finance.domain.repository.AccountMappingRepository;
import com.maiu.erp.modules.finance.domain.repository.AccountRepository;
import com.maiu.erp.modules.finance.domain.repository.JournalEntryRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.ConflictException;
import com.maiu.erp.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;

/** Finance-user actions: chart of accounts, account mappings, manual entries and reversals. */
@Service
public class FinanceAdminService {
    // Automatic postings are corrected through the stock transaction that made them, not here.
    private static final Set<JournalSourceType> MANUALLY_REVERSIBLE =
            EnumSet.of(JournalSourceType.MANUAL, JournalSourceType.OPENING_BALANCE);

    private final AccountRepository accountRepository;
    private final AccountMappingRepository accountMappingRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final LedgerPostingService ledgerPostingService;

    public FinanceAdminService(
            AccountRepository accountRepository,
            AccountMappingRepository accountMappingRepository,
            JournalEntryRepository journalEntryRepository,
            LedgerPostingService ledgerPostingService) {
        this.accountRepository = accountRepository;
        this.accountMappingRepository = accountMappingRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.ledgerPostingService = ledgerPostingService;
    }

    @Transactional
    public Account createAccount(CreateAccountRequest request) {
        String code = request.code().trim();
        if (accountRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Account code " + code + " already exists");
        }
        Account account = new Account();
        account.setCode(code);
        account.setName(request.name().trim());
        account.setType(request.type());
        account.setActive(true);
        account.setCreatedAt(Instant.now());
        return accountRepository.save(account);
    }

    /** Code and type are fixed once created, so posted history keeps its meaning. */
    @Transactional
    public Account updateAccount(UUID id, UpdateAccountRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        if (!request.active() && accountMappingRepository.findAll().containsValue(id)) {
            throw new BadRequestException("Account " + account.getCode()
                    + " is used by automatic postings; remap it before deactivating");
        }
        account.setName(request.name().trim());
        account.setActive(request.active());
        return accountRepository.save(account);
    }

    @Transactional
    public void updateAccountMapping(String key, UUID accountId) {
        AccountMappingKey mappingKey;
        try {
            mappingKey = AccountMappingKey.valueOf(key.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown account mapping: " + key);
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        if (!account.isActive()) {
            throw new BadRequestException("Account " + account.getCode() + " is inactive");
        }
        accountMappingRepository.save(mappingKey, accountId);
    }

    @Transactional
    public JournalEntry createManualEntry(CreateJournalEntryRequest request) {
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(request.entryDate());
        entry.setSourceType(JournalSourceType.MANUAL);
        entry.setMemo(request.memo() == null || request.memo().isBlank() ? null : request.memo().trim());
        entry.setCreatedBy(CurrentActor.name());
        List<JournalLine> lines = new ArrayList<>();
        for (CreateJournalLineRequest lineRequest : request.lines()) {
            JournalLine line = new JournalLine();
            line.setAccountId(lineRequest.accountId());
            line.setDebit(lineRequest.debit());
            line.setCredit(lineRequest.credit());
            line.setProjectId(lineRequest.projectId());
            line.setMemo(lineRequest.memo() == null || lineRequest.memo().isBlank() ? null : lineRequest.memo().trim());
            lines.add(line);
        }
        entry.setLines(lines);
        return ledgerPostingService.post(entry);
    }

    @Transactional
    public JournalEntry reverseEntry(UUID id, ReverseJournalEntryRequest request) {
        JournalEntry original = journalEntryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Journal entry not found"));
        if (!MANUALLY_REVERSIBLE.contains(original.getSourceType())) {
            throw new BadRequestException(
                    "Automatic entries are corrected through their source transaction (for example a return)");
        }
        return ledgerPostingService.reverse(
                id,
                request == null ? null : request.reversalDate(),
                request == null ? null : request.memo(),
                CurrentActor.name());
    }
}
