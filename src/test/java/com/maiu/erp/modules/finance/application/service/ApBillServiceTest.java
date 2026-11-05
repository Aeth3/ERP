package com.maiu.erp.modules.finance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.maiu.erp.modules.finance.application.port.PurchaseOrderReader;
import com.maiu.erp.modules.finance.application.service.ApBillService.CreateBillRequest;
import com.maiu.erp.modules.finance.application.service.ApBillService.LineReq;
import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;
import com.maiu.erp.modules.finance.domain.enums.AccountType;
import com.maiu.erp.modules.finance.domain.enums.BillStatus;
import com.maiu.erp.modules.finance.domain.enums.FiscalPeriodStatus;
import com.maiu.erp.modules.finance.domain.enums.JournalEntryStatus;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;
import com.maiu.erp.modules.finance.domain.model.Account;
import com.maiu.erp.modules.finance.domain.model.Bill;
import com.maiu.erp.modules.finance.domain.model.FiscalPeriod;
import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.model.JournalLine;
import com.maiu.erp.modules.finance.testsupport.InMemoryAccountMappingRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryAccountRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryBillRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryFiscalPeriodRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryJournalEntryRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.ConflictException;

class ApBillServiceTest {
    private static final LocalDate BILL_DATE = LocalDate.of(2026, 11, 5);

    private InMemoryBillRepository billRepository;
    private InMemoryAccountRepository accountRepository;
    private InMemoryAccountMappingRepository mappingRepository;
    private InMemoryJournalEntryRepository journalEntryRepository;
    private InMemoryFiscalPeriodRepository fiscalPeriodRepository;
    private StubPurchaseOrderReader purchaseOrderReader;
    private ApBillService service;
    private final Map<AccountMappingKey, UUID> accountIds = new HashMap<>();
    private UUID expenseAccountId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("ap-clerk", "n/a", "ROLE_FINANCE"));
        billRepository = new InMemoryBillRepository();
        accountRepository = new InMemoryAccountRepository();
        mappingRepository = new InMemoryAccountMappingRepository();
        journalEntryRepository = new InMemoryJournalEntryRepository();
        fiscalPeriodRepository = new InMemoryFiscalPeriodRepository();
        purchaseOrderReader = new StubPurchaseOrderReader();

        mappedAccount("2000", AccountType.LIABILITY, AccountMappingKey.ACCOUNTS_PAYABLE);
        mappedAccount("2010", AccountType.LIABILITY, AccountMappingKey.GOODS_RECEIVED_NOT_INVOICED);
        mappedAccount("5150", AccountType.EXPENSE, AccountMappingKey.PURCHASE_PRICE_VARIANCE);
        expenseAccountId = plainAccount("6100", AccountType.EXPENSE).getId();

        LedgerPostingService ledgerPostingService = new LedgerPostingService(
                accountRepository, journalEntryRepository, new FiscalPeriodService(fiscalPeriodRepository));
        service = new ApBillService(
                billRepository, purchaseOrderReader, ledgerPostingService, mappingRepository, "Asia/Manila");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- draft validation ---

    @Test
    void createDraft_rejectsMixedLineTypes() {
        PoFixture po = receivedPo("10.00", "20.00");
        LineReq poLine = poLine(po.itemId(), "1.00", "20.00");
        LineReq expenseLine = expenseLine("50.00");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.createDraft(request(po.supplierId(), po.poId(), "INV-1", List.of(poLine, expenseLine))));
        assertTrue(ex.getMessage().toLowerCase().contains("line"));
    }

    @Test
    void createDraft_rejectsNonPositiveQuantityOrAmount() {
        PoFixture po = receivedPo("10.00", "20.00");

        assertThrows(BadRequestException.class, () -> service.createDraft(
                request(po.supplierId(), po.poId(), "INV-1", List.of(poLine(po.itemId(), "0.00", "20.00")))));
        assertThrows(BadRequestException.class, () -> service.createDraft(
                request(UUID.randomUUID(), null, "INV-2", List.of(expenseLine("-5.00")))));
    }

    @Test
    void createDraft_rejectsDuplicateBillNumberForSupplier() {
        UUID supplierId = UUID.randomUUID();
        service.createDraft(request(supplierId, null, "INV-9", List.of(expenseLine("10.00"))));

        assertThrows(ConflictException.class, () -> service.createDraft(
                request(supplierId, null, "INV-9", List.of(expenseLine("20.00")))));
        // Same number for a different supplier is allowed.
        service.createDraft(request(UUID.randomUUID(), null, "INV-9", List.of(expenseLine("20.00"))));
    }

    // --- 3-way match ---

    @Test
    void approve_blocksWhenPoNotReceived() {
        PoFixture po = receivedPo("10.00", "20.00");
        purchaseOrderReader.setStatus(po.poId(), "APPROVED");
        Bill bill = service.createDraft(
                request(po.supplierId(), po.poId(), "INV-1", List.of(poLine(po.itemId(), "1.00", "20.00"))));

        assertThrows(BadRequestException.class, () -> service.approve(bill.getId()));
    }

    @Test
    void approve_blocksSupplierMismatch() {
        PoFixture po = receivedPo("10.00", "20.00");
        Bill bill = service.createDraft(
                request(UUID.randomUUID(), po.poId(), "INV-1", List.of(poLine(po.itemId(), "1.00", "20.00"))));

        assertThrows(BadRequestException.class, () -> service.approve(bill.getId()));
    }

    @Test
    void approve_blocksOverBillingNetOfReturns() {
        // 10 ordered, 3 returned -> 7 billable.
        PoFixture po = receivedPo("10.00", "20.00");
        purchaseOrderReader.setReturnedQuantity(po.poId(), po.itemId(), new BigDecimal("3.00"));

        Bill first = service.createDraft(
                request(po.supplierId(), po.poId(), "INV-1", List.of(poLine(po.itemId(), "5.00", "20.00"))));
        service.approve(first.getId());

        Bill second = service.createDraft(
                request(po.supplierId(), po.poId(), "INV-2", List.of(poLine(po.itemId(), "3.00", "20.00"))));
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.approve(second.getId()));
        assertTrue(ex.getMessage().toLowerCase().contains("exceed"));
    }

    // --- posting ---

    @Test
    void approve_postsGrniPpvAndAp() {
        PoFixture po = receivedPo("10.00", "20.00");
        Bill bill = service.createDraft(
                request(po.supplierId(), po.poId(), "INV-1", List.of(poLine(po.itemId(), "10.00", "22.00"))));

        Bill approved = service.approve(bill.getId());

        assertEquals(BillStatus.APPROVED, approved.getStatus());
        JournalEntry entry = journalEntryRepository.findById(approved.getJournalEntryId()).orElseThrow();
        assertEquals(JournalSourceType.AP_BILL, entry.getSourceType());
        assertEquals(bill.getId(), entry.getSourceId());
        // Dr GRNI 200.00, Dr PPV 20.00, Cr AP 220.00
        assertEquals(new BigDecimal("200.00"), debit(entry, AccountMappingKey.GOODS_RECEIVED_NOT_INVOICED));
        assertEquals(new BigDecimal("20.00"), debit(entry, AccountMappingKey.PURCHASE_PRICE_VARIANCE));
        assertEquals(new BigDecimal("220.00"), credit(entry, AccountMappingKey.ACCOUNTS_PAYABLE));
    }

    @Test
    void approve_postsPpvCreditWhenBelowPoCost() {
        PoFixture po = receivedPo("10.00", "20.00");
        Bill bill = service.createDraft(
                request(po.supplierId(), po.poId(), "INV-1", List.of(poLine(po.itemId(), "10.00", "18.00"))));

        Bill approved = service.approve(bill.getId());

        JournalEntry entry = journalEntryRepository.findById(approved.getJournalEntryId()).orElseThrow();
        assertEquals(new BigDecimal("200.00"), debit(entry, AccountMappingKey.GOODS_RECEIVED_NOT_INVOICED));
        assertEquals(new BigDecimal("20.00"), credit(entry, AccountMappingKey.PURCHASE_PRICE_VARIANCE));
        assertEquals(new BigDecimal("180.00"), credit(entry, AccountMappingKey.ACCOUNTS_PAYABLE));
    }

    @Test
    void approve_omitsPpvLineWhenExactMatch() {
        PoFixture po = receivedPo("10.00", "20.00");
        Bill bill = service.createDraft(
                request(po.supplierId(), po.poId(), "INV-1", List.of(poLine(po.itemId(), "10.00", "20.00"))));

        Bill approved = service.approve(bill.getId());

        JournalEntry entry = journalEntryRepository.findById(approved.getJournalEntryId()).orElseThrow();
        UUID ppvAccountId = accountIds.get(AccountMappingKey.PURCHASE_PRICE_VARIANCE);
        assertTrue(entry.getLines().stream().noneMatch(line -> ppvAccountId.equals(line.getAccountId())));
        assertEquals(new BigDecimal("200.00"), credit(entry, AccountMappingKey.ACCOUNTS_PAYABLE));
    }

    @Test
    void approve_nonPoBillPostsExpenseLines() {
        UUID supplierId = UUID.randomUUID();
        Bill bill = service.createDraft(request(supplierId, null, "RENT-11", List.of(expenseLine("5000.00"))));

        Bill approved = service.approve(bill.getId());

        JournalEntry entry = journalEntryRepository.findById(approved.getJournalEntryId()).orElseThrow();
        JournalLine expense = entry.getLines().stream()
                .filter(line -> expenseAccountId.equals(line.getAccountId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("5000.00"), expense.getDebit());
        assertEquals(new BigDecimal("5000.00"), credit(entry, AccountMappingKey.ACCOUNTS_PAYABLE));
    }

    @Test
    void approve_rollsBackWhenPeriodClosed() {
        FiscalPeriod closed = new FiscalPeriod();
        closed.setYear(BILL_DATE.getYear());
        closed.setMonth(BILL_DATE.getMonthValue());
        closed.setStartDate(BILL_DATE.withDayOfMonth(1));
        closed.setEndDate(BILL_DATE.withDayOfMonth(30));
        closed.setStatus(FiscalPeriodStatus.CLOSED);
        fiscalPeriodRepository.save(closed);

        UUID supplierId = UUID.randomUUID();
        Bill bill = service.createDraft(request(supplierId, null, "INV-1", List.of(expenseLine("100.00"))));

        assertThrows(BadRequestException.class, () -> service.approve(bill.getId()));

        Bill reloaded = billRepository.findById(bill.getId()).orElseThrow();
        assertEquals(BillStatus.DRAFT, reloaded.getStatus());
        assertNull(reloaded.getJournalEntryId());
    }

    // --- cancel ---

    @Test
    void cancel_approvedUnpaidReversesJournal() {
        UUID supplierId = UUID.randomUUID();
        Bill bill = service.createDraft(request(supplierId, null, "INV-1", List.of(expenseLine("100.00"))));
        Bill approved = service.approve(bill.getId());

        Bill cancelled = service.cancel(approved.getId());

        assertEquals(BillStatus.CANCELLED, cancelled.getStatus());
        JournalEntry original = journalEntryRepository.findById(approved.getJournalEntryId()).orElseThrow();
        assertEquals(JournalEntryStatus.REVERSED, original.getStatus());
    }

    @Test
    void cancel_rejectedWhenPaid() {
        UUID supplierId = UUID.randomUUID();
        Bill bill = service.createDraft(request(supplierId, null, "INV-1", List.of(expenseLine("100.00"))));
        Bill approved = service.approve(bill.getId());
        approved.setAmountPaid(new BigDecimal("40.00"));
        approved.setStatus(BillStatus.PARTIALLY_PAID);
        billRepository.save(approved);

        assertThrows(BadRequestException.class, () -> service.cancel(approved.getId()));
    }

    @Test
    void cancel_releasesBilledQuantityForMatch() {
        PoFixture po = receivedPo("10.00", "20.00");
        Bill first = service.createDraft(
                request(po.supplierId(), po.poId(), "INV-1", List.of(poLine(po.itemId(), "10.00", "20.00"))));
        service.approve(first.getId());
        service.cancel(first.getId());

        Bill second = service.createDraft(
                request(po.supplierId(), po.poId(), "INV-2", List.of(poLine(po.itemId(), "10.00", "20.00"))));
        assertEquals(BillStatus.APPROVED, service.approve(second.getId()).getStatus());
    }

    // --- helpers ---

    private CreateBillRequest request(UUID supplierId, UUID poId, String billNumber, List<LineReq> lines) {
        return new CreateBillRequest(supplierId, poId, billNumber, BILL_DATE, BILL_DATE.plusDays(30), null, lines);
    }

    private LineReq poLine(UUID poItemId, String quantity, String unitCost) {
        return new LineReq(poItemId, new BigDecimal(quantity), new BigDecimal(unitCost), null, null, null);
    }

    private LineReq expenseLine(String amount) {
        return new LineReq(null, null, null, expenseAccountId, new BigDecimal(amount), "expense");
    }

    private BigDecimal debit(JournalEntry entry, AccountMappingKey key) {
        UUID accountId = accountIds.get(key);
        return entry.getLines().stream()
                .filter(line -> accountId.equals(line.getAccountId()))
                .map(JournalLine::getDebit)
                .filter(amount -> amount != null && amount.signum() != 0)
                .findFirst().orElse(BigDecimal.ZERO);
    }

    private BigDecimal credit(JournalEntry entry, AccountMappingKey key) {
        UUID accountId = accountIds.get(key);
        return entry.getLines().stream()
                .filter(line -> accountId.equals(line.getAccountId()))
                .map(JournalLine::getCredit)
                .filter(amount -> amount != null && amount.signum() != 0)
                .findFirst().orElse(BigDecimal.ZERO);
    }

    private Account plainAccount(String code, AccountType type) {
        Account account = new Account();
        account.setCode(code);
        account.setName("Account " + code);
        account.setType(type);
        account.setActive(true);
        return accountRepository.save(account);
    }

    private void mappedAccount(String code, AccountType type, AccountMappingKey key) {
        Account saved = plainAccount(code, type);
        mappingRepository.save(key, saved.getId());
        accountIds.put(key, saved.getId());
    }

    /** One received PO with a single item. */
    private PoFixture receivedPo(String quantity, String unitCost) {
        UUID poId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        purchaseOrderReader.add(new PurchaseOrderReader.PoSnapshot(
                poId, "PO-" + poId.toString().substring(0, 4), supplierId, "RECEIVED",
                List.of(new PurchaseOrderReader.PoLine(
                        itemId, UUID.randomUUID(), new BigDecimal(quantity), BigDecimal.ZERO,
                        new BigDecimal(unitCost)))));
        return new PoFixture(poId, itemId, supplierId);
    }

    private record PoFixture(UUID poId, UUID itemId, UUID supplierId) {
    }

    private static final class StubPurchaseOrderReader implements PurchaseOrderReader {
        private final Map<UUID, PoSnapshot> snapshots = new HashMap<>();

        void add(PoSnapshot snapshot) {
            snapshots.put(snapshot.id(), snapshot);
        }

        void setStatus(UUID poId, String status) {
            PoSnapshot current = snapshots.get(poId);
            snapshots.put(poId, new PoSnapshot(
                    current.id(), current.poNumber(), current.supplierId(), status, current.lines()));
        }

        void setReturnedQuantity(UUID poId, UUID poItemId, BigDecimal returnedQuantity) {
            PoSnapshot current = snapshots.get(poId);
            List<PoLine> lines = new ArrayList<>();
            for (PoLine line : current.lines()) {
                lines.add(poItemId.equals(line.poItemId())
                        ? new PoLine(line.poItemId(), line.productId(), line.quantity(), returnedQuantity,
                                line.unitCost())
                        : line);
            }
            snapshots.put(poId, new PoSnapshot(
                    current.id(), current.poNumber(), current.supplierId(), current.status(), lines));
        }

        @Override
        public Optional<PoSnapshot> find(UUID purchaseOrderId) {
            return Optional.ofNullable(snapshots.get(purchaseOrderId));
        }
    }
}
