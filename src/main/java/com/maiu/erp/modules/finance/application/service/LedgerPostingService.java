package com.maiu.erp.modules.finance.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.finance.domain.enums.JournalEntryStatus;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;
import com.maiu.erp.modules.finance.domain.model.Account;
import com.maiu.erp.modules.finance.domain.model.FiscalPeriod;
import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.model.JournalLine;
import com.maiu.erp.modules.finance.domain.repository.AccountRepository;
import com.maiu.erp.modules.finance.domain.repository.JournalEntryRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.ConflictException;
import com.maiu.erp.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;

/**
 * The only way entries reach the ledger. Every entry is checked for balance, active accounts
 * and an open period before it is saved; posted entries are only ever reversed, never edited.
 */
@Service
public class LedgerPostingService {
    private static final DateTimeFormatter NUMBER_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final FiscalPeriodService fiscalPeriodService;

    public LedgerPostingService(
            AccountRepository accountRepository,
            JournalEntryRepository journalEntryRepository,
            FiscalPeriodService fiscalPeriodService) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.fiscalPeriodService = fiscalPeriodService;
    }

    @Transactional
    public JournalEntry post(JournalEntry draft) {
        if (draft.getEntryDate() == null) {
            throw new BadRequestException("Entry date is required");
        }
        if (draft.getSourceType() == null) {
            throw new BadRequestException("Source type is required");
        }
        if (draft.getCreatedBy() == null || draft.getCreatedBy().isBlank()) {
            throw new BadRequestException("createdBy is required");
        }
        if (draft.getStockMovementId() != null
                && journalEntryRepository.existsByStockMovementId(draft.getStockMovementId())) {
            throw new ConflictException("Stock movement is already posted to the ledger");
        }

        BigDecimal total = validateLines(draft.getLines());
        FiscalPeriod period = fiscalPeriodService.openPeriodFor(draft.getEntryDate());

        draft.setFiscalPeriodId(period.getId());
        draft.setEntryNumber(generateEntryNumber(draft.getEntryDate()));
        draft.setStatus(JournalEntryStatus.POSTED);
        draft.setTotalAmount(total);
        draft.setCreatedAt(Instant.now());
        return journalEntryRepository.save(draft);
    }

    /** Posts the mirror image of an entry and marks the original as reversed. */
    @Transactional
    public JournalEntry reverse(UUID entryId, LocalDate reversalDate, String memo, String actor) {
        JournalEntry original = journalEntryRepository.findById(entryId)
                .orElseThrow(() -> new NotFoundException("Journal entry not found"));
        if (original.getStatus() == JournalEntryStatus.REVERSED) {
            throw new BadRequestException("Journal entry is already reversed");
        }
        if (original.getSourceType() == JournalSourceType.REVERSAL) {
            throw new BadRequestException("A reversal entry cannot itself be reversed");
        }

        JournalEntry reversal = new JournalEntry();
        reversal.setEntryDate(reversalDate == null ? LocalDate.now() : reversalDate);
        reversal.setSourceType(JournalSourceType.REVERSAL);
        reversal.setSourceId(original.getId());
        reversal.setReversalOfId(original.getId());
        reversal.setMemo(memo == null || memo.isBlank()
                ? "Reversal of " + original.getEntryNumber()
                : memo.trim());
        reversal.setCreatedBy(actor);
        List<JournalLine> lines = new ArrayList<>();
        for (JournalLine line : original.getLines()) {
            JournalLine mirrored = new JournalLine();
            mirrored.setLineNumber(line.getLineNumber());
            mirrored.setAccountId(line.getAccountId());
            mirrored.setDebit(line.getCredit());
            mirrored.setCredit(line.getDebit());
            mirrored.setProjectId(line.getProjectId());
            mirrored.setWarehouseId(line.getWarehouseId());
            mirrored.setProductId(line.getProductId());
            mirrored.setMemo(line.getMemo());
            lines.add(mirrored);
        }
        reversal.setLines(lines);

        JournalEntry savedReversal = post(reversal);
        original.setStatus(JournalEntryStatus.REVERSED);
        original.setReversedById(savedReversal.getId());
        journalEntryRepository.save(original);
        return savedReversal;
    }

    private BigDecimal validateLines(List<JournalLine> lines) {
        if (lines == null || lines.size() < 2) {
            throw new BadRequestException("A journal entry needs at least two lines");
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        int lineNumber = 1;
        for (JournalLine line : lines) {
            BigDecimal debit = normalizeAmount(line.getDebit());
            BigDecimal credit = normalizeAmount(line.getCredit());
            if (debit.signum() < 0 || credit.signum() < 0) {
                throw new BadRequestException("Line " + lineNumber + ": amounts cannot be negative");
            }
            if (debit.signum() > 0 == credit.signum() > 0) {
                throw new BadRequestException("Line " + lineNumber + ": enter either a debit or a credit");
            }
            Account account = line.getAccountId() == null
                    ? null
                    : accountRepository.findById(line.getAccountId()).orElse(null);
            if (account == null) {
                throw new BadRequestException("Line " + lineNumber + ": account not found");
            }
            if (!account.isActive()) {
                throw new BadRequestException("Line " + lineNumber + ": account " + account.getCode() + " is inactive");
            }

            line.setDebit(debit);
            line.setCredit(credit);
            line.setLineNumber(lineNumber++);
            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BadRequestException(
                    "Entry is unbalanced: debits " + totalDebit + " vs credits " + totalCredit);
        }
        return totalDebit;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateEntryNumber(LocalDate entryDate) {
        String entryNumber;
        do {
            entryNumber = "JE-" + entryDate.format(NUMBER_MONTH) + "-"
                    + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (journalEntryRepository.existsByEntryNumber(entryNumber));
        return entryNumber;
    }
}
