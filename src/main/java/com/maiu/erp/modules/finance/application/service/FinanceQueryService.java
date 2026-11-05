package com.maiu.erp.modules.finance.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.finance.application.dto.AccountDto;
import com.maiu.erp.modules.finance.application.dto.AccountLedgerDto;
import com.maiu.erp.modules.finance.application.dto.AccountLedgerLineDto;
import com.maiu.erp.modules.finance.application.dto.AccountMappingDto;
import com.maiu.erp.modules.finance.application.dto.FiscalPeriodDto;
import com.maiu.erp.modules.finance.application.dto.InventoryReconciliationDto;
import com.maiu.erp.modules.finance.application.dto.JournalEntryDto;
import com.maiu.erp.modules.finance.application.dto.JournalLineDto;
import com.maiu.erp.modules.finance.application.dto.TrialBalanceDto;
import com.maiu.erp.modules.finance.application.dto.TrialBalanceRowDto;
import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;
import com.maiu.erp.modules.finance.domain.model.Account;
import com.maiu.erp.modules.finance.domain.model.FiscalPeriod;
import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.model.JournalLine;
import com.maiu.erp.modules.finance.domain.repository.AccountMappingRepository;
import com.maiu.erp.modules.finance.domain.repository.AccountRepository;
import com.maiu.erp.modules.finance.domain.repository.JournalEntryRepository;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.repository.InventoryStockRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.NotFoundException;

@Service
public class FinanceQueryService {
    private static final LocalDate LEDGER_START = LocalDate.of(1900, 1, 1);

    private final AccountRepository accountRepository;
    private final AccountMappingRepository accountMappingRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final FiscalPeriodService fiscalPeriodService;
    private final InventoryStockRepository inventoryStockRepository;

    public FinanceQueryService(
            AccountRepository accountRepository,
            AccountMappingRepository accountMappingRepository,
            JournalEntryRepository journalEntryRepository,
            FiscalPeriodService fiscalPeriodService,
            InventoryStockRepository inventoryStockRepository) {
        this.accountRepository = accountRepository;
        this.accountMappingRepository = accountMappingRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.fiscalPeriodService = fiscalPeriodService;
        this.inventoryStockRepository = inventoryStockRepository;
    }

    public List<AccountDto> getAccounts() {
        return accountRepository.findAll().stream()
                .sorted(Comparator.comparing(Account::getCode))
                .map(FinanceQueryService::toAccountDto)
                .toList();
    }

    public List<AccountMappingDto> getAccountMappings() {
        Map<UUID, Account> accounts = accountsById();
        Map<AccountMappingKey, UUID> mappings = accountMappingRepository.findAll();
        List<AccountMappingDto> result = new ArrayList<>();
        for (AccountMappingKey key : AccountMappingKey.values()) {
            UUID accountId = mappings.get(key);
            Account account = accountId == null ? null : accounts.get(accountId);
            result.add(new AccountMappingDto(
                    key,
                    accountId,
                    account == null ? null : account.getCode(),
                    account == null ? null : account.getName()));
        }
        return result;
    }

    public List<JournalEntryDto> getJournalEntries(LocalDate startDate, LocalDate endDate, String sourceType) {
        LocalDate start = startDate == null ? LEDGER_START : startDate;
        LocalDate end = endDate == null ? LocalDate.now().plusYears(100) : endDate;
        validateRange(start, end);
        JournalSourceType source = parseSourceType(sourceType);
        Map<UUID, Account> accounts = accountsById();

        return journalEntryRepository.findByDateRange(start, end).stream()
                .filter(entry -> source == null || source == entry.getSourceType())
                .sorted(Comparator.comparing(JournalEntry::getEntryDate)
                        .thenComparing(JournalEntry::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .map(entry -> toEntryDto(entry, accounts))
                .toList();
    }

    public JournalEntryDto getJournalEntry(UUID id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Journal entry not found"));
        return toEntryDto(entry, accountsById());
    }

    public List<JournalEntryDto> getJournalEntriesForSource(UUID sourceId) {
        Map<UUID, Account> accounts = accountsById();
        return journalEntryRepository.findBySourceId(sourceId).stream()
                .sorted(Comparator.comparing(JournalEntry::getEntryDate))
                .map(entry -> toEntryDto(entry, accounts))
                .toList();
    }

    public TrialBalanceDto getTrialBalance(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.withDayOfYear(1) : startDate;
        validateRange(start, end);

        Map<UUID, BigDecimal[]> totals = new HashMap<>();
        for (JournalEntry entry : journalEntryRepository.findByDateRange(LEDGER_START, end)) {
            boolean inRange = !entry.getEntryDate().isBefore(start);
            for (JournalLine line : entry.getLines()) {
                // [0] closing debit, [1] closing credit, [2] period debit, [3] period credit
                BigDecimal[] values = totals.computeIfAbsent(line.getAccountId(), id -> new BigDecimal[] {
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
                values[0] = values[0].add(line.getDebit());
                values[1] = values[1].add(line.getCredit());
                if (inRange) {
                    values[2] = values[2].add(line.getDebit());
                    values[3] = values[3].add(line.getCredit());
                }
            }
        }

        Map<UUID, Account> accounts = accountsById();
        List<TrialBalanceRowDto> rows = new ArrayList<>();
        BigDecimal totalPeriodDebit = BigDecimal.ZERO;
        BigDecimal totalPeriodCredit = BigDecimal.ZERO;
        BigDecimal totalDebitBalance = BigDecimal.ZERO;
        BigDecimal totalCreditBalance = BigDecimal.ZERO;
        for (Map.Entry<UUID, BigDecimal[]> item : totals.entrySet()) {
            Account account = accounts.get(item.getKey());
            BigDecimal[] values = item.getValue();
            BigDecimal net = values[0].subtract(values[1]);
            BigDecimal debitBalance = net.signum() > 0 ? net : BigDecimal.ZERO;
            BigDecimal creditBalance = net.signum() < 0 ? net.negate() : BigDecimal.ZERO;
            rows.add(new TrialBalanceRowDto(
                    item.getKey(),
                    account == null ? null : account.getCode(),
                    account == null ? null : account.getName(),
                    account == null ? null : account.getType(),
                    values[2],
                    values[3],
                    debitBalance,
                    creditBalance));
            totalPeriodDebit = totalPeriodDebit.add(values[2]);
            totalPeriodCredit = totalPeriodCredit.add(values[3]);
            totalDebitBalance = totalDebitBalance.add(debitBalance);
            totalCreditBalance = totalCreditBalance.add(creditBalance);
        }
        rows.sort(Comparator.comparing(TrialBalanceRowDto::code, Comparator.nullsLast(Comparator.naturalOrder())));

        return new TrialBalanceDto(
                start,
                end,
                rows,
                totalPeriodDebit,
                totalPeriodCredit,
                totalDebitBalance,
                totalCreditBalance,
                totalDebitBalance.compareTo(totalCreditBalance) == 0);
    }

    public AccountLedgerDto getAccountLedger(UUID accountId, LocalDate startDate, LocalDate endDate) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.withDayOfMonth(1) : startDate;
        validateRange(start, end);
        boolean debitNormal = account.getType().isDebitNormal();

        BigDecimal opening = BigDecimal.ZERO;
        List<AccountLedgerLineDto> lines = new ArrayList<>();
        List<JournalEntry> entries = journalEntryRepository.findByDateRange(LEDGER_START, end).stream()
                .sorted(Comparator.comparing(JournalEntry::getEntryDate)
                        .thenComparing(JournalEntry::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        for (JournalEntry entry : entries) {
            for (JournalLine line : entry.getLines()) {
                if (!accountId.equals(line.getAccountId())) {
                    continue;
                }
                BigDecimal signed = signedAmount(line, debitNormal);
                if (entry.getEntryDate().isBefore(start)) {
                    opening = opening.add(signed);
                }
            }
        }

        BigDecimal running = opening;
        for (JournalEntry entry : entries) {
            if (entry.getEntryDate().isBefore(start)) {
                continue;
            }
            for (JournalLine line : entry.getLines()) {
                if (!accountId.equals(line.getAccountId())) {
                    continue;
                }
                running = running.add(signedAmount(line, debitNormal));
                lines.add(new AccountLedgerLineDto(
                        entry.getId(),
                        entry.getEntryNumber(),
                        entry.getEntryDate(),
                        entry.getSourceType(),
                        entry.getMemo(),
                        line.getDebit(),
                        line.getCredit(),
                        running,
                        line.getProjectId(),
                        line.getWarehouseId()));
            }
        }

        return new AccountLedgerDto(toAccountDto(account), start, end, opening, lines, running);
    }

    public List<FiscalPeriodDto> getPeriods() {
        return fiscalPeriodService.getPeriods().stream().map(FinanceQueryService::toPeriodDto).toList();
    }

    /** Compares the inventory account with quantity x average cost across all stock rows. */
    public InventoryReconciliationDto getInventoryReconciliation() {
        UUID inventoryAccountId = accountMappingRepository.findAccountId(AccountMappingKey.INVENTORY)
                .orElseThrow(() -> new BadRequestException("Ledger account mapping INVENTORY is not configured"));

        Map<UUID, BigDecimal> ledgerByWarehouse = new HashMap<>();
        BigDecimal ledgerTotal = BigDecimal.ZERO;
        for (JournalEntry entry : journalEntryRepository.findByDateRange(LEDGER_START, LocalDate.now().plusYears(100))) {
            for (JournalLine line : entry.getLines()) {
                if (!inventoryAccountId.equals(line.getAccountId())) {
                    continue;
                }
                BigDecimal signed = line.getDebit().subtract(line.getCredit());
                ledgerTotal = ledgerTotal.add(signed);
                ledgerByWarehouse.merge(line.getWarehouseId(), signed, BigDecimal::add);
            }
        }

        Map<UUID, BigDecimal> stockByWarehouse = new HashMap<>();
        BigDecimal stockTotal = BigDecimal.ZERO;
        for (InventoryStock stock : inventoryStockRepository.findAll()) {
            BigDecimal averageCost = stock.getAverageCost() == null ? BigDecimal.ZERO : stock.getAverageCost();
            BigDecimal value = stock.getQuantityOnHand().multiply(averageCost).setScale(2, RoundingMode.HALF_UP);
            stockTotal = stockTotal.add(value);
            stockByWarehouse.merge(stock.getWarehouseId(), value, BigDecimal::add);
        }

        Set<UUID> warehouseIds = new HashSet<>(ledgerByWarehouse.keySet());
        warehouseIds.addAll(stockByWarehouse.keySet());
        List<InventoryReconciliationDto.WarehouseLine> warehouses = warehouseIds.stream()
                .filter(Objects::nonNull)
                .map(id -> {
                    BigDecimal ledger = ledgerByWarehouse.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal stock = stockByWarehouse.getOrDefault(id, BigDecimal.ZERO);
                    return new InventoryReconciliationDto.WarehouseLine(id, ledger, stock, ledger.subtract(stock));
                })
                .sorted(Comparator.comparing(line -> line.difference().abs(), Comparator.reverseOrder()))
                .toList();

        return new InventoryReconciliationDto(ledgerTotal, stockTotal, ledgerTotal.subtract(stockTotal), warehouses);
    }

    static AccountDto toAccountDto(Account account) {
        return new AccountDto(account.getId(), account.getCode(), account.getName(), account.getType(), account.isActive());
    }

    static FiscalPeriodDto toPeriodDto(FiscalPeriod period) {
        return new FiscalPeriodDto(
                period.getId(),
                period.getYear(),
                period.getMonth(),
                period.getStartDate(),
                period.getEndDate(),
                period.getStatus(),
                period.getClosedBy(),
                period.getClosedAt());
    }

    JournalEntryDto toEntryDto(JournalEntry entry, Map<UUID, Account> accounts) {
        List<JournalLineDto> lines = entry.getLines().stream()
                .map(line -> {
                    Account account = accounts.get(line.getAccountId());
                    return new JournalLineDto(
                            line.getId(),
                            line.getLineNumber(),
                            line.getAccountId(),
                            account == null ? null : account.getCode(),
                            account == null ? null : account.getName(),
                            line.getDebit(),
                            line.getCredit(),
                            line.getProjectId(),
                            line.getWarehouseId(),
                            line.getProductId(),
                            line.getMemo());
                })
                .toList();
        return new JournalEntryDto(
                entry.getId(),
                entry.getEntryNumber(),
                entry.getEntryDate(),
                entry.getSourceType(),
                entry.getSourceId(),
                entry.getStockMovementId(),
                entry.getMemo(),
                entry.getStatus(),
                entry.getReversalOfId(),
                entry.getReversedById(),
                entry.getTotalAmount(),
                entry.getCreatedBy(),
                entry.getCreatedAt(),
                lines);
    }

    Map<UUID, Account> accountsById() {
        return accountRepository.findAll().stream().collect(Collectors.toMap(Account::getId, Function.identity()));
    }

    private BigDecimal signedAmount(JournalLine line, boolean debitNormal) {
        BigDecimal net = line.getDebit().subtract(line.getCredit());
        return debitNormal ? net : net.negate();
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new BadRequestException("startDate cannot be after endDate");
        }
    }

    private JournalSourceType parseSourceType(String sourceType) {
        if (sourceType == null || sourceType.isBlank()) {
            return null;
        }
        try {
            return JournalSourceType.valueOf(sourceType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown sourceType: " + sourceType.trim());
        }
    }
}
