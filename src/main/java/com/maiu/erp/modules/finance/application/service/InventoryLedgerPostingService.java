package com.maiu.erp.modules.finance.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;
import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.model.JournalLine;
import com.maiu.erp.modules.finance.domain.repository.AccountMappingRepository;
import com.maiu.erp.modules.finance.domain.repository.JournalEntryRepository;
import com.maiu.erp.modules.inventory.application.port.InventoryLedgerEvent;
import com.maiu.erp.modules.inventory.application.port.InventoryLedgerPoster;
import com.maiu.erp.shared.exception.BadRequestException;

/**
 * Posts each valued stock movement as a two-line entry. Runs inside the stock transaction,
 * so stock and ledger change together or not at all. Zero-value movements post nothing.
 */
@Service
public class InventoryLedgerPostingService implements InventoryLedgerPoster {
    private final LedgerPostingService ledgerPostingService;
    private final AccountMappingRepository accountMappingRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final ZoneId businessZone;

    public InventoryLedgerPostingService(
            LedgerPostingService ledgerPostingService,
            AccountMappingRepository accountMappingRepository,
            JournalEntryRepository journalEntryRepository,
            @Value("${finance.time-zone:Asia/Manila}") String businessZone) {
        this.ledgerPostingService = ledgerPostingService;
        this.accountMappingRepository = accountMappingRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.businessZone = ZoneId.of(businessZone);
    }

    @Override
    public void post(InventoryLedgerEvent event) {
        if (event.quantity() == null || event.unitCost() == null) {
            return;
        }
        BigDecimal amount = event.quantity().multiply(event.unitCost()).setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            return;
        }
        if (event.stockMovementId() != null && journalEntryRepository.existsByStockMovementId(event.stockMovementId())) {
            return;
        }

        Rule rule = ruleFor(event.kind());
        Instant occurredAt = event.occurredAt() == null ? Instant.now() : event.occurredAt();

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(LocalDate.ofInstant(occurredAt, businessZone));
        entry.setSourceType(rule.sourceType());
        entry.setSourceId(event.sourceId());
        entry.setStockMovementId(event.stockMovementId());
        entry.setMemo(event.memo() == null || event.memo().isBlank()
                ? rule.description()
                : rule.description() + ": " + event.memo().trim());
        entry.setCreatedBy(event.performedBy() == null || event.performedBy().isBlank() ? "system" : event.performedBy());
        entry.setLines(List.of(
                line(accountFor(rule.debit()), amount, BigDecimal.ZERO, event, rule.description()),
                line(accountFor(rule.credit()), BigDecimal.ZERO, amount, event, rule.description())));

        ledgerPostingService.post(entry);
    }

    private JournalLine line(UUID accountId, BigDecimal debit, BigDecimal credit, InventoryLedgerEvent event, String memo) {
        JournalLine line = new JournalLine();
        line.setAccountId(accountId);
        line.setDebit(debit);
        line.setCredit(credit);
        line.setProjectId(event.projectId());
        line.setWarehouseId(event.warehouseId());
        line.setProductId(event.productId());
        line.setMemo(memo);
        return line;
    }

    private UUID accountFor(AccountMappingKey key) {
        return accountMappingRepository.findAccountId(key)
                .orElseThrow(() -> new BadRequestException("Ledger account mapping " + key + " is not configured"));
    }

    private Rule ruleFor(InventoryLedgerEvent.Kind kind) {
        return switch (kind) {
            case PURCHASE_RECEIPT -> new Rule(JournalSourceType.PURCHASE_RECEIPT, "Purchase receipt",
                    AccountMappingKey.INVENTORY, AccountMappingKey.GOODS_RECEIVED_NOT_INVOICED);
            case PURCHASE_RETURN -> new Rule(JournalSourceType.PURCHASE_RETURN, "Purchase return",
                    AccountMappingKey.GOODS_RECEIVED_NOT_INVOICED, AccountMappingKey.INVENTORY);
            case PROJECT_ISSUE -> new Rule(JournalSourceType.MATERIAL_ISSUE, "Material issued to project",
                    AccountMappingKey.PROJECT_MATERIAL_COST, AccountMappingKey.INVENTORY);
            case PROJECT_RETURN -> new Rule(JournalSourceType.MATERIAL_RETURN, "Material returned from project",
                    AccountMappingKey.INVENTORY, AccountMappingKey.PROJECT_MATERIAL_COST);
            case ADJUSTMENT_INCREASE -> new Rule(JournalSourceType.STOCK_ADJUSTMENT, "Stock adjustment increase",
                    AccountMappingKey.INVENTORY, AccountMappingKey.INVENTORY_ADJUSTMENT);
            case ADJUSTMENT_DECREASE -> new Rule(JournalSourceType.STOCK_ADJUSTMENT, "Stock adjustment decrease",
                    AccountMappingKey.INVENTORY_ADJUSTMENT, AccountMappingKey.INVENTORY);
            case SALES_SHIPMENT -> new Rule(JournalSourceType.SALES_SHIPMENT, "Sales shipment cost",
                    AccountMappingKey.COST_OF_SALES, AccountMappingKey.INVENTORY);
            case SALES_RETURN -> new Rule(JournalSourceType.SALES_RETURN, "Sales return cost",
                    AccountMappingKey.INVENTORY, AccountMappingKey.COST_OF_SALES);
        };
    }

    private record Rule(
            JournalSourceType sourceType,
            String description,
            AccountMappingKey debit,
            AccountMappingKey credit) {
    }
}
