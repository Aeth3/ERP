package com.maiu.erp.modules.finance.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;
import com.maiu.erp.modules.finance.domain.enums.AccountType;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;
import com.maiu.erp.modules.finance.domain.model.Account;
import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.model.JournalLine;
import com.maiu.erp.modules.finance.domain.repository.AccountMappingRepository;
import com.maiu.erp.modules.finance.domain.repository.AccountRepository;
import com.maiu.erp.modules.finance.domain.repository.JournalEntryRepository;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.repository.InventoryStockRepository;

import jakarta.transaction.Transactional;

/**
 * Brings the ledger to a usable state at startup: the default construction chart of accounts,
 * the account mappings automatic postings need, and a one-time opening inventory entry.
 * Only adds what is missing, so finance users' changes are never overwritten.
 */
@Service
public class FinanceSetupService {
    private static final List<DefaultAccount> DEFAULT_ACCOUNTS = List.of(
            new DefaultAccount("1000", "Cash on Hand", AccountType.ASSET),
            new DefaultAccount("1010", "Cash in Bank", AccountType.ASSET),
            new DefaultAccount("1100", "Accounts Receivable", AccountType.ASSET),
            new DefaultAccount("1110", "Retention Receivable", AccountType.ASSET),
            new DefaultAccount("1200", "Input VAT", AccountType.ASSET),
            new DefaultAccount("1300", "Inventory - Construction Materials", AccountType.ASSET),
            new DefaultAccount("1400", "Advances to Suppliers", AccountType.ASSET),
            new DefaultAccount("1500", "Property and Equipment", AccountType.ASSET),
            new DefaultAccount("1590", "Accumulated Depreciation", AccountType.ASSET),
            new DefaultAccount("2000", "Accounts Payable", AccountType.LIABILITY),
            new DefaultAccount("2010", "Goods Received Not Invoiced", AccountType.LIABILITY),
            new DefaultAccount("2100", "Output VAT Payable", AccountType.LIABILITY),
            new DefaultAccount("2110", "Expanded Withholding Tax Payable", AccountType.LIABILITY),
            new DefaultAccount("2200", "Customer Downpayments", AccountType.LIABILITY),
            new DefaultAccount("2210", "Retention Payable", AccountType.LIABILITY),
            new DefaultAccount("2300", "Accrued Expenses", AccountType.LIABILITY),
            new DefaultAccount("3000", "Owner's Capital", AccountType.EQUITY),
            new DefaultAccount("3100", "Retained Earnings", AccountType.EQUITY),
            new DefaultAccount("3900", "Opening Balance Equity", AccountType.EQUITY),
            new DefaultAccount("4000", "Contract Revenue", AccountType.REVENUE),
            new DefaultAccount("4100", "Sales - Materials", AccountType.REVENUE),
            new DefaultAccount("4900", "Other Income", AccountType.REVENUE),
            new DefaultAccount("5000", "Cost of Sales - Materials", AccountType.EXPENSE),
            new DefaultAccount("5100", "Contract Costs - Materials", AccountType.EXPENSE),
            new DefaultAccount("5200", "Contract Costs - Labor", AccountType.EXPENSE),
            new DefaultAccount("5300", "Contract Costs - Equipment", AccountType.EXPENSE),
            new DefaultAccount("5150", "Purchase Price Variance", AccountType.EXPENSE),
            new DefaultAccount("5400", "Contract Costs - Subcontract", AccountType.EXPENSE),
            new DefaultAccount("5900", "Inventory Adjustments", AccountType.EXPENSE),
            new DefaultAccount("6000", "Salaries and Wages", AccountType.EXPENSE),
            new DefaultAccount("6100", "Rent Expense", AccountType.EXPENSE),
            new DefaultAccount("6200", "Utilities Expense", AccountType.EXPENSE),
            new DefaultAccount("6900", "Miscellaneous Expense", AccountType.EXPENSE));

    private static final Map<AccountMappingKey, String> DEFAULT_MAPPINGS = Map.ofEntries(
            Map.entry(AccountMappingKey.INVENTORY, "1300"),
            Map.entry(AccountMappingKey.GOODS_RECEIVED_NOT_INVOICED, "2010"),
            Map.entry(AccountMappingKey.PROJECT_MATERIAL_COST, "5100"),
            Map.entry(AccountMappingKey.INVENTORY_ADJUSTMENT, "5900"),
            Map.entry(AccountMappingKey.COST_OF_SALES, "5000"),
            Map.entry(AccountMappingKey.OPENING_BALANCE_EQUITY, "3900"),
            Map.entry(AccountMappingKey.ACCOUNTS_PAYABLE, "2000"),
            Map.entry(AccountMappingKey.PURCHASE_PRICE_VARIANCE, "5150"),
            Map.entry(AccountMappingKey.CASH, "1000"),
            Map.entry(AccountMappingKey.BANK, "1010"),
            Map.entry(AccountMappingKey.ACCOUNTS_RECEIVABLE, "1100"),
            Map.entry(AccountMappingKey.RETENTION_RECEIVABLE, "1110"),
            Map.entry(AccountMappingKey.SALES_REVENUE, "4100"),
            Map.entry(AccountMappingKey.CONTRACT_REVENUE, "4000"));

    private final AccountRepository accountRepository;
    private final AccountMappingRepository accountMappingRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final LedgerPostingService ledgerPostingService;
    private final ZoneId businessZone;

    public FinanceSetupService(
            AccountRepository accountRepository,
            AccountMappingRepository accountMappingRepository,
            JournalEntryRepository journalEntryRepository,
            InventoryStockRepository inventoryStockRepository,
            LedgerPostingService ledgerPostingService,
            @Value("${finance.time-zone:Asia/Manila}") String businessZone) {
        this.accountRepository = accountRepository;
        this.accountMappingRepository = accountMappingRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.inventoryStockRepository = inventoryStockRepository;
        this.ledgerPostingService = ledgerPostingService;
        this.businessZone = ZoneId.of(businessZone);
    }

    @Transactional
    public void ensureDefaults() {
        for (DefaultAccount defaultAccount : DEFAULT_ACCOUNTS) {
            if (accountRepository.findByCode(defaultAccount.code()).isEmpty()) {
                Account account = new Account();
                account.setCode(defaultAccount.code());
                account.setName(defaultAccount.name());
                account.setType(defaultAccount.type());
                account.setActive(true);
                account.setCreatedAt(Instant.now());
                accountRepository.save(account);
            }
        }

        Map<AccountMappingKey, UUID> mappings = accountMappingRepository.findAll();
        DEFAULT_MAPPINGS.forEach((key, code) -> {
            if (!mappings.containsKey(key)) {
                accountRepository.findByCode(code)
                        .ifPresent(account -> accountMappingRepository.save(key, account.getId()));
            }
        });

        postOpeningBalanceIfLedgerIsEmpty();
    }

    /**
     * At go-live the ledger is empty while stock already has value. Book that value once:
     * debit inventory per warehouse, credit opening balance equity. Later movements post themselves.
     */
    private void postOpeningBalanceIfLedgerIsEmpty() {
        if (journalEntryRepository.existsAny()) {
            return;
        }

        Map<UUID, BigDecimal> valueByWarehouse = new TreeMap<>();
        for (InventoryStock stock : inventoryStockRepository.findAll()) {
            BigDecimal averageCost = stock.getAverageCost() == null ? BigDecimal.ZERO : stock.getAverageCost();
            BigDecimal value = stock.getQuantityOnHand().multiply(averageCost).setScale(2, RoundingMode.HALF_UP);
            if (value.signum() > 0) {
                valueByWarehouse.merge(stock.getWarehouseId(), value, BigDecimal::add);
            }
        }
        if (valueByWarehouse.isEmpty()) {
            return;
        }

        UUID inventoryAccountId = accountMappingRepository.findAccountId(AccountMappingKey.INVENTORY).orElse(null);
        UUID equityAccountId = accountMappingRepository.findAccountId(AccountMappingKey.OPENING_BALANCE_EQUITY).orElse(null);
        if (inventoryAccountId == null || equityAccountId == null) {
            return;
        }

        List<JournalLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<UUID, BigDecimal> item : valueByWarehouse.entrySet()) {
            JournalLine line = new JournalLine();
            line.setAccountId(inventoryAccountId);
            line.setDebit(item.getValue());
            line.setWarehouseId(item.getKey());
            line.setMemo("Opening stock value");
            lines.add(line);
            total = total.add(item.getValue());
        }
        JournalLine equityLine = new JournalLine();
        equityLine.setAccountId(equityAccountId);
        equityLine.setCredit(total);
        equityLine.setMemo("Opening balance equity");
        lines.add(equityLine);

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(LocalDate.now(businessZone));
        entry.setSourceType(JournalSourceType.OPENING_BALANCE);
        entry.setMemo("Opening inventory balance at general ledger go-live");
        entry.setCreatedBy("system");
        entry.setLines(lines);
        ledgerPostingService.post(entry);
    }

    private record DefaultAccount(String code, String name, AccountType type) {
    }
}
