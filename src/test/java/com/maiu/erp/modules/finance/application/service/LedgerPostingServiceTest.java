package com.maiu.erp.modules.finance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.finance.application.dto.TrialBalanceDto;
import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;
import com.maiu.erp.modules.finance.domain.enums.AccountType;
import com.maiu.erp.modules.finance.domain.enums.JournalEntryStatus;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;
import com.maiu.erp.modules.finance.domain.model.Account;
import com.maiu.erp.modules.finance.domain.model.FiscalPeriod;
import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.model.JournalLine;
import com.maiu.erp.modules.finance.domain.repository.AccountMappingRepository;
import com.maiu.erp.modules.finance.domain.repository.AccountRepository;
import com.maiu.erp.modules.finance.domain.repository.FiscalPeriodRepository;
import com.maiu.erp.modules.finance.domain.repository.JournalEntryRepository;
import com.maiu.erp.modules.inventory.application.port.InventoryLedgerEvent;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.repository.InventoryStockRepository;
import com.maiu.erp.shared.exception.BadRequestException;

class LedgerPostingServiceTest {
    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    private InMemoryAccountRepository accountRepository;
    private InMemoryAccountMappingRepository mappingRepository;
    private InMemoryJournalEntryRepository journalEntryRepository;
    private InMemoryFiscalPeriodRepository fiscalPeriodRepository;
    private InMemoryInventoryStockRepository inventoryStockRepository;
    private LedgerPostingService ledgerPostingService;
    private InventoryLedgerPostingService inventoryPoster;
    private FinanceQueryService financeQueryService;
    private Map<String, Account> accountsByCode;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        mappingRepository = new InMemoryAccountMappingRepository();
        journalEntryRepository = new InMemoryJournalEntryRepository();
        fiscalPeriodRepository = new InMemoryFiscalPeriodRepository();
        inventoryStockRepository = new InMemoryInventoryStockRepository();
        accountsByCode = new HashMap<>();

        account("1300", AccountType.ASSET, AccountMappingKey.INVENTORY);
        account("2010", AccountType.LIABILITY, AccountMappingKey.GOODS_RECEIVED_NOT_INVOICED);
        account("5100", AccountType.EXPENSE, AccountMappingKey.PROJECT_MATERIAL_COST);
        account("5900", AccountType.EXPENSE, AccountMappingKey.INVENTORY_ADJUSTMENT);
        account("5000", AccountType.EXPENSE, AccountMappingKey.COST_OF_SALES);
        account("3900", AccountType.EQUITY, AccountMappingKey.OPENING_BALANCE_EQUITY);

        FiscalPeriodService fiscalPeriodService = new FiscalPeriodService(fiscalPeriodRepository);
        ledgerPostingService = new LedgerPostingService(accountRepository, journalEntryRepository, fiscalPeriodService);
        inventoryPoster = new InventoryLedgerPostingService(
                ledgerPostingService, mappingRepository, journalEntryRepository, "Asia/Manila");
        financeQueryService = new FinanceQueryService(
                accountRepository, mappingRepository, journalEntryRepository, fiscalPeriodService, inventoryStockRepository);
    }

    @Test
    void purchaseReceiptDebitsInventoryAndCreditsGoodsReceivedNotInvoiced() {
        UUID movementId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();

        inventoryPoster.post(event(InventoryLedgerEvent.Kind.PURCHASE_RECEIPT, movementId, warehouseId, null, "10", "125.50"));

        JournalEntry entry = journalEntryRepository.entries.getFirst();
        assertEquals(JournalSourceType.PURCHASE_RECEIPT, entry.getSourceType());
        assertEquals(new BigDecimal("1255.00"), entry.getTotalAmount());
        assertEquals(JournalEntryStatus.POSTED, entry.getStatus());
        assertLine(entry.getLines().get(0), "1300", "1255.00", "0.00");
        assertLine(entry.getLines().get(1), "2010", "0.00", "1255.00");
        assertEquals(warehouseId, entry.getLines().get(0).getWarehouseId());
    }

    @Test
    void materialIssueChargesTheProjectAndIsPostedOnlyOncePerMovement() {
        UUID movementId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        InventoryLedgerEvent issue = event(
                InventoryLedgerEvent.Kind.PROJECT_ISSUE, movementId, UUID.randomUUID(), projectId, "4", "15.00");
        inventoryPoster.post(issue);
        inventoryPoster.post(issue);

        assertEquals(1, journalEntryRepository.entries.size());
        JournalEntry entry = journalEntryRepository.entries.getFirst();
        assertLine(entry.getLines().get(0), "5100", "60.00", "0.00");
        assertLine(entry.getLines().get(1), "1300", "0.00", "60.00");
        assertEquals(projectId, entry.getLines().get(0).getProjectId());
    }

    @Test
    void zeroValueMovementPostsNothing() {
        inventoryPoster.post(event(
                InventoryLedgerEvent.Kind.SALES_RETURN, UUID.randomUUID(), UUID.randomUUID(), null, "3", "0"));

        assertTrue(journalEntryRepository.entries.isEmpty());
    }

    @Test
    void unbalancedEntryIsRejected() {
        JournalEntry entry = manualEntry(line("1300", "100.00", null), line("3900", null, "99.99"));

        BadRequestException error = assertThrows(BadRequestException.class, () -> ledgerPostingService.post(entry));
        assertTrue(error.getMessage().contains("unbalanced"));
        assertTrue(journalEntryRepository.entries.isEmpty());
    }

    @Test
    void lineWithBothDebitAndCreditIsRejected() {
        JournalEntry entry = manualEntry(line("1300", "100.00", "100.00"), line("3900", null, null));

        assertThrows(BadRequestException.class, () -> ledgerPostingService.post(entry));
    }

    @Test
    void inactiveAccountIsRejected() {
        accountsByCode.get("3900").setActive(false);
        JournalEntry entry = manualEntry(line("1300", "100.00", null), line("3900", null, "100.00"));

        assertThrows(BadRequestException.class, () -> ledgerPostingService.post(entry));
    }

    @Test
    void closedPeriodRefusesPostings() {
        LocalDate today = LocalDate.now(MANILA);
        FiscalPeriod closed = new FiscalPeriod();
        closed.setYear(today.getYear());
        closed.setMonth(today.getMonthValue());
        closed.setStartDate(today.withDayOfMonth(1));
        closed.setEndDate(today.withDayOfMonth(today.lengthOfMonth()));
        closed.setStatus(com.maiu.erp.modules.finance.domain.enums.FiscalPeriodStatus.CLOSED);
        fiscalPeriodRepository.save(closed);

        assertThrows(BadRequestException.class, () -> inventoryPoster.post(event(
                InventoryLedgerEvent.Kind.PURCHASE_RECEIPT, UUID.randomUUID(), UUID.randomUUID(), null, "1", "10")));
    }

    @Test
    void reversalMirrorsLinesAndCannotBeRepeated() {
        JournalEntry posted = ledgerPostingService.post(
                manualEntry(line("1300", "500.00", null), line("3900", null, "500.00")));

        JournalEntry reversal = ledgerPostingService.reverse(posted.getId(), null, null, "controller");

        assertEquals(JournalSourceType.REVERSAL, reversal.getSourceType());
        assertEquals(posted.getId(), reversal.getReversalOfId());
        assertLine(reversal.getLines().get(0), "1300", "0.00", "500.00");
        assertLine(reversal.getLines().get(1), "3900", "500.00", "0.00");
        assertEquals(JournalEntryStatus.REVERSED, journalEntryRepository.findById(posted.getId()).orElseThrow().getStatus());
        assertThrows(BadRequestException.class,
                () -> ledgerPostingService.reverse(posted.getId(), null, null, "controller"));
    }

    @Test
    void trialBalanceBalancesAndInventoryReconcilesWithStockValue() {
        UUID warehouseId = UUID.randomUUID();
        inventoryPoster.post(event(InventoryLedgerEvent.Kind.PURCHASE_RECEIPT, UUID.randomUUID(), warehouseId, null, "20", "15.00"));
        inventoryPoster.post(event(InventoryLedgerEvent.Kind.PROJECT_ISSUE, UUID.randomUUID(), warehouseId, UUID.randomUUID(), "4", "15.00"));
        inventoryPoster.post(event(InventoryLedgerEvent.Kind.SALES_SHIPMENT, UUID.randomUUID(), warehouseId, null, "6", "15.00"));

        TrialBalanceDto trialBalance = financeQueryService.getTrialBalance(null, null);
        assertTrue(trialBalance.balanced());
        assertEquals(new BigDecimal("300.00"), trialBalance.totalDebitBalance());

        // 20 - 4 - 6 = 10 units left at 15.00 = 150.00, matching the inventory account.
        InventoryStock stock = new InventoryStock();
        stock.setWarehouseId(warehouseId);
        stock.setQuantityOnHand(new BigDecimal("10"));
        stock.setAverageCost(new BigDecimal("15.0000"));
        inventoryStockRepository.stocks.add(stock);

        var reconciliation = financeQueryService.getInventoryReconciliation();
        assertEquals(new BigDecimal("150.00"), reconciliation.ledgerBalance());
        assertEquals(0, reconciliation.difference().signum());
    }

    @Test
    void setupAddsMissingAccountsAndPostsTheOpeningBalanceOnlyOnce() {
        UUID warehouseA = UUID.randomUUID();
        UUID warehouseB = UUID.randomUUID();
        inventoryStockRepository.stocks.add(costedStock(warehouseA, "10", "12.5000"));
        inventoryStockRepository.stocks.add(costedStock(warehouseB, "4", "100.0000"));
        UUID customInventoryAccount = accountsByCode.get("1300").getId();
        FinanceSetupService setup = new FinanceSetupService(
                accountRepository, mappingRepository, journalEntryRepository, inventoryStockRepository,
                ledgerPostingService, "Asia/Manila");

        setup.ensureDefaults();
        setup.ensureDefaults();

        assertEquals(33, accountRepository.findAll().size());
        assertEquals(customInventoryAccount, mappingRepository.findAccountId(AccountMappingKey.INVENTORY).orElseThrow());
        assertEquals(1, journalEntryRepository.entries.size());
        JournalEntry opening = journalEntryRepository.entries.getFirst();
        assertEquals(JournalSourceType.OPENING_BALANCE, opening.getSourceType());
        assertEquals(new BigDecimal("525.00"), opening.getTotalAmount());
        assertEquals(3, opening.getLines().size());
        assertLine(opening.getLines().get(2), "3900", "0.00", "525.00");
    }

    private InventoryStock costedStock(UUID warehouseId, String quantity, String averageCost) {
        InventoryStock stock = new InventoryStock();
        stock.setWarehouseId(warehouseId);
        stock.setQuantityOnHand(new BigDecimal(quantity));
        stock.setAverageCost(new BigDecimal(averageCost));
        return stock;
    }

    private void assertLine(JournalLine line, String accountCode, String debit, String credit) {
        assertEquals(accountsByCode.get(accountCode).getId(), line.getAccountId(), "account");
        assertEquals(new BigDecimal(debit), line.getDebit(), "debit");
        assertEquals(new BigDecimal(credit), line.getCredit(), "credit");
    }

    private InventoryLedgerEvent event(
            InventoryLedgerEvent.Kind kind,
            UUID movementId,
            UUID warehouseId,
            UUID projectId,
            String quantity,
            String unitCost) {
        return new InventoryLedgerEvent(
                kind,
                movementId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                warehouseId,
                projectId,
                new BigDecimal(quantity),
                new BigDecimal(unitCost),
                null,
                "storekeeper",
                Instant.now());
    }

    private JournalEntry manualEntry(JournalLine... lines) {
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(LocalDate.now(MANILA));
        entry.setSourceType(JournalSourceType.MANUAL);
        entry.setCreatedBy("controller");
        entry.setLines(new ArrayList<>(List.of(lines)));
        return entry;
    }

    private JournalLine line(String accountCode, String debit, String credit) {
        JournalLine line = new JournalLine();
        line.setAccountId(accountsByCode.get(accountCode).getId());
        line.setDebit(debit == null ? null : new BigDecimal(debit));
        line.setCredit(credit == null ? null : new BigDecimal(credit));
        return line;
    }

    private void account(String code, AccountType type, AccountMappingKey mappingKey) {
        Account account = new Account();
        account.setCode(code);
        account.setName("Account " + code);
        account.setType(type);
        account.setActive(true);
        Account saved = accountRepository.save(account);
        accountsByCode.put(code, saved);
        mappingRepository.save(mappingKey, saved.getId());
    }

    private static final class InMemoryAccountRepository implements AccountRepository {
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

    private static final class InMemoryAccountMappingRepository implements AccountMappingRepository {
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

    private static final class InMemoryJournalEntryRepository implements JournalEntryRepository {
        private final List<JournalEntry> entries = new ArrayList<>();

        @Override
        public JournalEntry save(JournalEntry entry) {
            if (entry.getId() == null) {
                entry.setId(UUID.randomUUID());
                entries.add(entry);
            }
            return entry;
        }

        @Override
        public Optional<JournalEntry> findById(UUID id) {
            return entries.stream().filter(entry -> id.equals(entry.getId())).findFirst();
        }

        @Override
        public boolean existsByEntryNumber(String entryNumber) {
            return entries.stream().anyMatch(entry -> entryNumber.equals(entry.getEntryNumber()));
        }

        @Override
        public boolean existsByStockMovementId(UUID stockMovementId) {
            return entries.stream().anyMatch(entry -> stockMovementId.equals(entry.getStockMovementId()));
        }

        @Override
        public boolean existsAny() {
            return !entries.isEmpty();
        }

        @Override
        public List<JournalEntry> findByDateRange(LocalDate startDate, LocalDate endDate) {
            return entries.stream()
                    .filter(entry -> !entry.getEntryDate().isBefore(startDate) && !entry.getEntryDate().isAfter(endDate))
                    .toList();
        }

        @Override
        public List<JournalEntry> findBySourceId(UUID sourceId) {
            return entries.stream().filter(entry -> sourceId.equals(entry.getSourceId())).toList();
        }
    }

    private static final class InMemoryFiscalPeriodRepository implements FiscalPeriodRepository {
        private final List<FiscalPeriod> periods = new ArrayList<>();

        @Override
        public FiscalPeriod save(FiscalPeriod period) {
            if (period.getId() == null) {
                period.setId(UUID.randomUUID());
                periods.add(period);
            }
            return period;
        }

        @Override
        public Optional<FiscalPeriod> findById(UUID id) {
            return periods.stream().filter(period -> id.equals(period.getId())).findFirst();
        }

        @Override
        public Optional<FiscalPeriod> findByYearAndMonth(int year, int month) {
            return periods.stream()
                    .filter(period -> period.getYear() == year && period.getMonth() == month)
                    .findFirst();
        }

        @Override
        public List<FiscalPeriod> findAll() {
            return List.copyOf(periods);
        }
    }

    private static final class InMemoryInventoryStockRepository implements InventoryStockRepository {
        private final List<InventoryStock> stocks = new ArrayList<>();

        @Override
        public InventoryStock save(InventoryStock stock) {
            stocks.add(stock);
            return stock;
        }

        @Override
        public List<InventoryStock> findAll() {
            return List.copyOf(stocks);
        }

        @Override
        public Optional<InventoryStock> findByProductIdAndWarehouseId(UUID productId, UUID warehouseId) {
            return Optional.empty();
        }

        @Override
        public List<InventoryStock> findByProductId(UUID productId) {
            return List.of();
        }

        @Override
        public List<InventoryStock> findByWarehouseId(UUID warehouseId) {
            return List.of();
        }
    }
}
