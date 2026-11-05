package com.maiu.erp.modules.finance.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.finance.application.port.PurchaseOrderReader;
import com.maiu.erp.modules.finance.application.port.PurchaseOrderReader.PoLine;
import com.maiu.erp.modules.finance.application.port.PurchaseOrderReader.PoSnapshot;
import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;
import com.maiu.erp.modules.finance.domain.enums.AccountType;
import com.maiu.erp.modules.finance.domain.enums.BillStatus;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;
import com.maiu.erp.modules.finance.domain.model.Account;
import com.maiu.erp.modules.finance.domain.model.Bill;
import com.maiu.erp.modules.finance.domain.model.BillLine;
import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.model.JournalLine;
import com.maiu.erp.modules.finance.domain.repository.AccountMappingRepository;
import com.maiu.erp.modules.finance.domain.repository.AccountRepository;
import com.maiu.erp.modules.finance.domain.repository.BillRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.ConflictException;
import com.maiu.erp.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;

/**
 * Supplier bills. PO-backed bills are 3-way matched (PO, received status, bill) at approval;
 * approval posts to the ledger in the same transaction, so a posting failure keeps the bill DRAFT.
 */
@Service
public class ApBillService {
    private static final int MONEY_SCALE = 2;

    private final BillRepository billRepository;
    private final PurchaseOrderReader purchaseOrderReader;
    private final LedgerPostingService ledgerPostingService;
    private final AccountMappingRepository accountMappingRepository;
    private final AccountRepository accountRepository;
    private final ZoneId businessZone;

    public ApBillService(
            BillRepository billRepository,
            PurchaseOrderReader purchaseOrderReader,
            LedgerPostingService ledgerPostingService,
            AccountMappingRepository accountMappingRepository,
            AccountRepository accountRepository,
            @Value("${finance.time-zone:Asia/Manila}") String businessZone) {
        this.billRepository = billRepository;
        this.purchaseOrderReader = purchaseOrderReader;
        this.ledgerPostingService = ledgerPostingService;
        this.accountMappingRepository = accountMappingRepository;
        this.accountRepository = accountRepository;
        this.businessZone = ZoneId.of(businessZone);
    }

    public record CreateBillRequest(
            UUID supplierId,
            UUID purchaseOrderId,
            String billNumber,
            LocalDate billDate,
            LocalDate dueDate,
            String notes,
            List<LineReq> lines) {
    }

    public record LineReq(
            UUID purchaseOrderItemId,
            BigDecimal quantity,
            BigDecimal unitCost,
            UUID accountId,
            BigDecimal amount,
            String description) {
    }

    @Transactional
    public Bill createDraft(CreateBillRequest request) {
        validateRequest(request);
        if (billRepository.existsBySupplierIdAndBillNumberIgnoreCaseAndStatusNot(
                request.supplierId(), billNumberOf(request), BillStatus.CANCELLED)) {
            throw new ConflictException("Supplier already has a bill numbered " + billNumberOf(request));
        }
        Bill bill = new Bill();
        bill.setStatus(BillStatus.DRAFT);
        bill.setCreatedBy(CurrentActor.name());
        bill.setCreatedAt(Instant.now());
        applyRequest(bill, request);
        return billRepository.save(bill);
    }

    @Transactional
    public Bill updateDraft(UUID id, CreateBillRequest request) {
        Bill bill = getForUpdate(id);
        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BadRequestException("Only draft bills can be edited");
        }
        validateRequest(request);
        boolean numberChanged = !bill.getBillNumber().equalsIgnoreCase(billNumberOf(request))
                || !bill.getSupplierId().equals(request.supplierId());
        if (numberChanged && billRepository.existsBySupplierIdAndBillNumberIgnoreCaseAndStatusNot(
                request.supplierId(), billNumberOf(request), BillStatus.CANCELLED)) {
            throw new ConflictException("Supplier already has a bill numbered " + billNumberOf(request));
        }
        applyRequest(bill, request);
        return billRepository.save(bill);
    }

    @Transactional
    public Bill approve(UUID id) {
        Bill bill = getForUpdate(id);
        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BadRequestException("Only draft bills can be approved");
        }

        JournalEntry draft;
        if (bill.getPurchaseOrderId() != null) {
            PoSnapshot snapshot = purchaseOrderReader.find(bill.getPurchaseOrderId())
                    .orElseThrow(() -> new NotFoundException("Purchase order not found"));
            matchAgainstPo(bill, snapshot);
            draft = poBillEntry(bill, snapshot);
        } else {
            draft = expenseBillEntry(bill);
        }

        JournalEntry posted = ledgerPostingService.post(draft);
        bill.setJournalEntryId(posted.getId());
        bill.setStatus(BillStatus.APPROVED);
        return billRepository.save(bill);
    }

    @Transactional
    public Bill cancel(UUID id) {
        Bill bill = getForUpdate(id);
        switch (bill.getStatus()) {
            case DRAFT -> bill.setStatus(BillStatus.CANCELLED);
            case APPROVED -> {
                if (bill.getAmountPaid() != null && bill.getAmountPaid().signum() != 0) {
                    throw new BadRequestException(
                            "Bill " + bill.getBillNumber() + " has payments and cannot be cancelled");
                }
                ledgerPostingService.reverse(
                        bill.getJournalEntryId(),
                        LocalDate.now(businessZone),
                        "Cancellation of bill " + bill.getBillNumber(),
                        CurrentActor.name());
                bill.setStatus(BillStatus.CANCELLED);
            }
            case CANCELLED -> throw new BadRequestException(
                    "Bill " + bill.getBillNumber() + " is already cancelled");
            default -> throw new BadRequestException(
                    "Bill " + bill.getBillNumber() + " has payments and cannot be cancelled");
        }
        return billRepository.save(bill);
    }

    public Bill get(UUID id) {
        return billRepository.findById(id).orElseThrow(() -> new NotFoundException("Bill not found"));
    }

    private Bill getForUpdate(UUID id) {
        return billRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Bill not found"));
    }

    private static String billNumberOf(CreateBillRequest request) {
        return request.billNumber() == null ? null : request.billNumber().trim();
    }

    private static void requireMoneyScale(BigDecimal value, String field) {
        if (value.stripTrailingZeros().scale() > MONEY_SCALE) {
            throw new BadRequestException("Line " + field + " must have at most two decimal places");
        }
    }

    public record BillableLine(
            UUID poItemId,
            UUID productId,
            String productName,
            BigDecimal orderedQuantity,
            BigDecimal returnedQuantity,
            BigDecimal billedQuantity,
            BigDecimal remainingQuantity,
            BigDecimal unitCost) {
    }

    /** What is still billable on a PO: ordered minus returned minus already billed, per item. */
    public List<BillableLine> billableLines(UUID purchaseOrderId) {
        PoSnapshot snapshot = purchaseOrderReader.find(purchaseOrderId)
                .orElseThrow(() -> new NotFoundException("Purchase order not found"));

        Map<UUID, BigDecimal> billed = new HashMap<>();
        for (Bill other : billRepository.findByPurchaseOrderId(purchaseOrderId)) {
            if (other.getStatus() == BillStatus.CANCELLED) {
                continue;
            }
            for (BillLine line : other.getLines()) {
                billed.merge(line.getPurchaseOrderItemId(), line.getQuantity(), BigDecimal::add);
            }
        }

        List<BillableLine> lines = new ArrayList<>();
        for (PoLine poLine : snapshot.lines()) {
            BigDecimal billedQuantity = billed.getOrDefault(poLine.poItemId(), BigDecimal.ZERO);
            BigDecimal remaining = poLine.quantity().subtract(poLine.returnedQuantity()).subtract(billedQuantity);
            lines.add(new BillableLine(
                    poLine.poItemId(),
                    poLine.productId(),
                    poLine.productName(),
                    poLine.quantity(),
                    poLine.returnedQuantity(),
                    billedQuantity,
                    remaining.max(BigDecimal.ZERO),
                    poLine.unitCost()));
        }
        return lines;
    }

    public List<Bill> search(UUID supplierId, BillStatus status, UUID purchaseOrderId) {
        return billRepository.search(supplierId, status, purchaseOrderId);
    }

    public BigDecimal remainingDue(Bill bill) {
        BigDecimal paid = bill.getAmountPaid() == null ? BigDecimal.ZERO : bill.getAmountPaid();
        return bill.getTotal().subtract(paid);
    }

    // --- validation ---

    private void validateRequest(CreateBillRequest request) {
        if (request.supplierId() == null) {
            throw new BadRequestException("Supplier is required");
        }
        if (request.billNumber() == null || request.billNumber().isBlank()) {
            throw new BadRequestException("Bill number is required");
        }
        if (request.billDate() == null) {
            throw new BadRequestException("Bill date is required");
        }
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new BadRequestException("A bill needs at least one line");
        }

        boolean poBacked = request.purchaseOrderId() != null;
        for (LineReq line : request.lines()) {
            if (poBacked) {
                if (line.purchaseOrderItemId() == null || line.accountId() != null) {
                    throw new BadRequestException(
                            "Bill lines must all reference the purchase order, or the bill must have no PO");
                }
                if (line.quantity() == null || line.quantity().signum() <= 0) {
                    throw new BadRequestException("Line quantity must be positive");
                }
                if (line.unitCost() == null || line.unitCost().signum() < 0) {
                    throw new BadRequestException("Line unit cost must not be negative");
                }
                requireMoneyScale(line.quantity(), "quantity");
                requireMoneyScale(line.unitCost(), "unit cost");
            } else {
                if (line.accountId() == null || line.purchaseOrderItemId() != null) {
                    throw new BadRequestException(
                            "Bill lines must all reference the purchase order, or the bill must have no PO");
                }
                if (line.amount() == null || line.amount().signum() <= 0) {
                    throw new BadRequestException("Line amount must be positive");
                }
                requireMoneyScale(line.amount(), "amount");
                Account account = accountRepository.findById(line.accountId())
                        .orElseThrow(() -> new BadRequestException("Unknown account on bill line"));
                if (account.getType() != AccountType.EXPENSE) {
                    throw new BadRequestException(
                            "Account " + account.getCode() + " is not an expense account");
                }
            }
        }
    }

    private void applyRequest(Bill bill, CreateBillRequest request) {
        bill.setSupplierId(request.supplierId());
        bill.setPurchaseOrderId(request.purchaseOrderId());
        bill.setBillNumber(billNumberOf(request));
        bill.setBillDate(request.billDate());
        bill.setDueDate(request.dueDate());
        bill.setNotes(request.notes());

        Map<UUID, PoLine> poLinesByItemId = new HashMap<>();
        if (request.purchaseOrderId() != null) {
            PoSnapshot snapshot = purchaseOrderReader.find(request.purchaseOrderId())
                    .orElseThrow(() -> new NotFoundException("Purchase order not found"));
            for (PoLine poLine : snapshot.lines()) {
                poLinesByItemId.put(poLine.poItemId(), poLine);
            }
        }

        List<BillLine> lines = new ArrayList<>();
        int lineNumber = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (LineReq lineReq : request.lines()) {
            BillLine line = new BillLine();
            line.setLineNumber(lineNumber++);
            line.setDescription(lineReq.description());
            if (request.purchaseOrderId() != null) {
                PoLine poLine = poLinesByItemId.get(lineReq.purchaseOrderItemId());
                if (poLine == null) {
                    throw new BadRequestException("Bill line does not reference an item of the purchase order");
                }
                line.setPurchaseOrderItemId(lineReq.purchaseOrderItemId());
                line.setProductId(poLine.productId());
                line.setQuantity(lineReq.quantity());
                line.setUnitCost(lineReq.unitCost());
                line.setLineTotal(money(lineReq.quantity().multiply(lineReq.unitCost())));
            } else {
                line.setAccountId(lineReq.accountId());
                line.setLineTotal(money(lineReq.amount()));
            }
            total = total.add(line.getLineTotal());
            lines.add(line);
        }
        bill.setLines(lines);
        bill.setSubtotal(total);
        bill.setTotal(total);
    }

    // --- 3-way match ---

    private void matchAgainstPo(Bill bill, PoSnapshot snapshot) {
        if (!"RECEIVED".equals(snapshot.status())) {
            throw new BadRequestException("Purchase order " + snapshot.poNumber() + " has not been received");
        }
        if (!snapshot.supplierId().equals(bill.getSupplierId())) {
            throw new BadRequestException("Bill supplier does not match purchase order supplier");
        }

        Map<UUID, PoLine> poLinesByItemId = new HashMap<>();
        for (PoLine line : snapshot.lines()) {
            poLinesByItemId.put(line.poItemId(), line);
        }

        Map<UUID, BigDecimal> alreadyBilled = new HashMap<>();
        for (Bill other : billRepository.findByPurchaseOrderId(bill.getPurchaseOrderId())) {
            if (other.getStatus() == BillStatus.CANCELLED || other.getId().equals(bill.getId())) {
                continue;
            }
            for (BillLine line : other.getLines()) {
                alreadyBilled.merge(line.getPurchaseOrderItemId(), line.getQuantity(), BigDecimal::add);
            }
        }

        for (BillLine line : bill.getLines()) {
            PoLine poLine = poLinesByItemId.get(line.getPurchaseOrderItemId());
            if (poLine == null) {
                throw new BadRequestException("Bill line does not reference an item of the purchase order");
            }
            BigDecimal billable = poLine.quantity().subtract(poLine.returnedQuantity());
            // merge() keeps repeated lines for the same PO item cumulative within this bill too.
            BigDecimal cumulative = alreadyBilled.merge(
                    line.getPurchaseOrderItemId(), line.getQuantity(), BigDecimal::add);
            if (cumulative.compareTo(billable) > 0) {
                throw new BadRequestException(
                        "Billed quantity would exceed the billable quantity (ordered minus returned) for "
                                + poLine.productName());
            }
        }
    }

    // --- posting ---

    private JournalEntry poBillEntry(Bill bill, PoSnapshot snapshot) {
        Map<UUID, PoLine> poLinesByItemId = new HashMap<>();
        for (PoLine line : snapshot.lines()) {
            poLinesByItemId.put(line.poItemId(), line);
        }

        BigDecimal grniAmount = BigDecimal.ZERO;
        for (BillLine line : bill.getLines()) {
            PoLine poLine = poLinesByItemId.get(line.getPurchaseOrderItemId());
            grniAmount = grniAmount.add(money(line.getQuantity().multiply(poLine.unitCost())));
        }
        BigDecimal variance = bill.getTotal().subtract(grniAmount);

        List<JournalLine> lines = new ArrayList<>();
        lines.add(journalLine(mappedAccount(AccountMappingKey.GOODS_RECEIVED_NOT_INVOICED), grniAmount, null));
        if (variance.signum() > 0) {
            lines.add(journalLine(mappedAccount(AccountMappingKey.PURCHASE_PRICE_VARIANCE), variance, null));
        } else if (variance.signum() < 0) {
            lines.add(journalLine(mappedAccount(AccountMappingKey.PURCHASE_PRICE_VARIANCE), null, variance.negate()));
        }
        lines.add(journalLine(mappedAccount(AccountMappingKey.ACCOUNTS_PAYABLE), null, bill.getTotal()));
        return entry(bill, "Bill " + bill.getBillNumber() + " for PO " + snapshot.poNumber(), lines);
    }

    private JournalEntry expenseBillEntry(Bill bill) {
        List<JournalLine> lines = new ArrayList<>();
        for (BillLine line : bill.getLines()) {
            lines.add(journalLine(line.getAccountId(), line.getLineTotal(), null));
        }
        lines.add(journalLine(mappedAccount(AccountMappingKey.ACCOUNTS_PAYABLE), null, bill.getTotal()));
        return entry(bill, "Bill " + bill.getBillNumber(), lines);
    }

    private JournalEntry entry(Bill bill, String memo, List<JournalLine> lines) {
        JournalEntry draft = new JournalEntry();
        draft.setEntryDate(bill.getBillDate());
        draft.setSourceType(JournalSourceType.AP_BILL);
        draft.setSourceId(bill.getId());
        draft.setMemo(memo);
        draft.setCreatedBy(CurrentActor.name());
        draft.setLines(lines);
        return draft;
    }

    private JournalLine journalLine(UUID accountId, BigDecimal debit, BigDecimal credit) {
        JournalLine line = new JournalLine();
        line.setAccountId(accountId);
        line.setDebit(debit);
        line.setCredit(credit);
        return line;
    }

    private UUID mappedAccount(AccountMappingKey key) {
        return accountMappingRepository.findAccountId(key)
                .orElseThrow(() -> new BadRequestException("No account mapped for " + key));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
