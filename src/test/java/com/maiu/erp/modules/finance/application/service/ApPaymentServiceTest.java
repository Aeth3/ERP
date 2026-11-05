package com.maiu.erp.modules.finance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.maiu.erp.modules.finance.application.service.ApPaymentService.AllocationReq;
import com.maiu.erp.modules.finance.application.service.ApPaymentService.CreatePaymentRequest;
import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;
import com.maiu.erp.modules.finance.domain.enums.AccountType;
import com.maiu.erp.modules.finance.domain.enums.BillStatus;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;
import com.maiu.erp.modules.finance.domain.enums.PaymentMethod;
import com.maiu.erp.modules.finance.domain.model.Account;
import com.maiu.erp.modules.finance.domain.model.Bill;
import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.model.JournalLine;
import com.maiu.erp.modules.finance.domain.model.Payment;
import com.maiu.erp.modules.finance.testsupport.InMemoryAccountMappingRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryAccountRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryBillRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryFiscalPeriodRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryJournalEntryRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryPaymentRepository;
import com.maiu.erp.shared.exception.BadRequestException;

class ApPaymentServiceTest {
    private static final LocalDate PAY_DATE = LocalDate.of(2026, 11, 10);

    private InMemoryBillRepository billRepository;
    private InMemoryPaymentRepository paymentRepository;
    private InMemoryJournalEntryRepository journalEntryRepository;
    private ApPaymentService service;
    private final Map<AccountMappingKey, UUID> accountIds = new HashMap<>();
    private InMemoryAccountRepository accountRepository;
    private InMemoryAccountMappingRepository mappingRepository;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("ap-clerk", "n/a", "ROLE_FINANCE"));
        billRepository = new InMemoryBillRepository();
        paymentRepository = new InMemoryPaymentRepository();
        accountRepository = new InMemoryAccountRepository();
        mappingRepository = new InMemoryAccountMappingRepository();
        journalEntryRepository = new InMemoryJournalEntryRepository();

        mappedAccount("2000", AccountType.LIABILITY, AccountMappingKey.ACCOUNTS_PAYABLE);
        mappedAccount("1000", AccountType.ASSET, AccountMappingKey.CASH);
        mappedAccount("1010", AccountType.ASSET, AccountMappingKey.BANK);

        LedgerPostingService ledgerPostingService = new LedgerPostingService(
                accountRepository, journalEntryRepository,
                new FiscalPeriodService(new InMemoryFiscalPeriodRepository()));
        service = new ApPaymentService(paymentRepository, billRepository, ledgerPostingService, mappingRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_rejectsAllocationExceedingRemainingDue() {
        Bill bill = approvedBill(UUID.randomUUID(), "100.00", "40.00");

        assertThrows(BadRequestException.class, () -> service.create(new CreatePaymentRequest(
                bill.getSupplierId(), PAY_DATE, PaymentMethod.BANK, null,
                List.of(new AllocationReq(bill.getId(), new BigDecimal("61.00"))))));
    }

    @Test
    void create_rejectsBillOfOtherSupplier() {
        Bill bill = approvedBill(UUID.randomUUID(), "100.00", "0.00");

        assertThrows(BadRequestException.class, () -> service.create(new CreatePaymentRequest(
                UUID.randomUUID(), PAY_DATE, PaymentMethod.BANK, null,
                List.of(new AllocationReq(bill.getId(), new BigDecimal("50.00"))))));
    }

    @Test
    void create_rejectsUnapprovedBill() {
        Bill bill = approvedBill(UUID.randomUUID(), "100.00", "0.00");
        bill.setStatus(BillStatus.DRAFT);
        billRepository.save(bill);

        assertThrows(BadRequestException.class, () -> service.create(new CreatePaymentRequest(
                bill.getSupplierId(), PAY_DATE, PaymentMethod.BANK, null,
                List.of(new AllocationReq(bill.getId(), new BigDecimal("50.00"))))));
    }

    @Test
    void create_partialPaymentSetsPartiallyPaid() {
        Bill bill = approvedBill(UUID.randomUUID(), "100.00", "0.00");

        Payment payment = service.create(new CreatePaymentRequest(
                bill.getSupplierId(), PAY_DATE, PaymentMethod.CASH, "or-77",
                List.of(new AllocationReq(bill.getId(), new BigDecimal("40.00")))));

        assertEquals(new BigDecimal("40.00"), payment.getTotal());
        assertTrue(payment.getPaymentNumber().startsWith("PAY-202611-"));
        Bill reloaded = billRepository.findById(bill.getId()).orElseThrow();
        assertEquals(BillStatus.PARTIALLY_PAID, reloaded.getStatus());
        assertEquals(new BigDecimal("40.00"), reloaded.getAmountPaid());

        JournalEntry entry = journalEntryRepository.findById(payment.getJournalEntryId()).orElseThrow();
        assertEquals(JournalSourceType.AP_PAYMENT, entry.getSourceType());
        assertEquals(new BigDecimal("40.00"), debit(entry, AccountMappingKey.ACCOUNTS_PAYABLE));
        assertEquals(new BigDecimal("40.00"), credit(entry, AccountMappingKey.CASH));
    }

    @Test
    void create_fullPaymentAcrossTwoBillsSetsPaid() {
        UUID supplierId = UUID.randomUUID();
        Bill first = approvedBill(supplierId, "100.00", "60.00");
        Bill second = approvedBill(supplierId, "50.00", "0.00");

        Payment payment = service.create(new CreatePaymentRequest(
                supplierId, PAY_DATE, PaymentMethod.BANK, null,
                List.of(new AllocationReq(first.getId(), new BigDecimal("40.00")),
                        new AllocationReq(second.getId(), new BigDecimal("50.00")))));

        assertEquals(new BigDecimal("90.00"), payment.getTotal());
        assertEquals(BillStatus.PAID, billRepository.findById(first.getId()).orElseThrow().getStatus());
        assertEquals(BillStatus.PAID, billRepository.findById(second.getId()).orElseThrow().getStatus());

        JournalEntry entry = journalEntryRepository.findById(payment.getJournalEntryId()).orElseThrow();
        assertEquals(new BigDecimal("90.00"), credit(entry, AccountMappingKey.BANK));
    }

    @Test
    void create_checkPostsToBank() {
        Bill bill = approvedBill(UUID.randomUUID(), "75.00", "0.00");

        Payment payment = service.create(new CreatePaymentRequest(
                bill.getSupplierId(), PAY_DATE, PaymentMethod.CHECK, "chk-9",
                List.of(new AllocationReq(bill.getId(), new BigDecimal("75.00")))));

        JournalEntry entry = journalEntryRepository.findById(payment.getJournalEntryId()).orElseThrow();
        assertEquals(new BigDecimal("75.00"), credit(entry, AccountMappingKey.BANK));
    }

    @Test
    void create_secondPaymentCannotOverpay() {
        Bill bill = approvedBill(UUID.randomUUID(), "100.00", "0.00");
        service.create(new CreatePaymentRequest(
                bill.getSupplierId(), PAY_DATE, PaymentMethod.BANK, null,
                List.of(new AllocationReq(bill.getId(), new BigDecimal("70.00")))));

        assertThrows(BadRequestException.class, () -> service.create(new CreatePaymentRequest(
                bill.getSupplierId(), PAY_DATE, PaymentMethod.BANK, null,
                List.of(new AllocationReq(bill.getId(), new BigDecimal("31.00"))))));
    }

    // --- minors cleanup pass ---

    @Test
    void create_rejectsAllocationWithMoreThanTwoDecimals() {
        Bill bill = approvedBill(UUID.randomUUID(), "100.00", "0.00");

        assertThrows(BadRequestException.class, () -> service.create(new CreatePaymentRequest(
                bill.getSupplierId(), PAY_DATE, PaymentMethod.BANK, null,
                List.of(new AllocationReq(bill.getId(), new BigDecimal("40.005"))))));
    }

    // --- review-pass regressions ---

    @Test
    void create_generatesSequentialPaymentNumbers() {
        Bill bill = approvedBill(UUID.randomUUID(), "100.00", "0.00");

        Payment first = service.create(new CreatePaymentRequest(
                bill.getSupplierId(), PAY_DATE, PaymentMethod.BANK, null,
                List.of(new AllocationReq(bill.getId(), new BigDecimal("30.00")))));
        Payment second = service.create(new CreatePaymentRequest(
                bill.getSupplierId(), PAY_DATE, PaymentMethod.BANK, null,
                List.of(new AllocationReq(bill.getId(), new BigDecimal("30.00")))));

        assertEquals("PAY-202611-0001", first.getPaymentNumber());
        assertEquals("PAY-202611-0002", second.getPaymentNumber());
    }

    @Test
    void create_loadsBillsWithUpdateLock() {
        Bill bill = approvedBill(UUID.randomUUID(), "100.00", "0.00");

        service.create(new CreatePaymentRequest(
                bill.getSupplierId(), PAY_DATE, PaymentMethod.BANK, null,
                List.of(new AllocationReq(bill.getId(), new BigDecimal("40.00")))));

        assertTrue(billRepository.getForUpdateCalls() > 0,
                "payment must load bills with findByIdForUpdate");
    }

    // --- helpers ---

    private Bill approvedBill(UUID supplierId, String total, String amountPaid) {
        Bill bill = new Bill();
        bill.setBillNumber("INV-" + UUID.randomUUID().toString().substring(0, 6));
        bill.setSupplierId(supplierId);
        bill.setBillDate(LocalDate.of(2026, 11, 1));
        bill.setDueDate(LocalDate.of(2026, 12, 1));
        BigDecimal paid = new BigDecimal(amountPaid);
        bill.setStatus(paid.signum() > 0 ? BillStatus.PARTIALLY_PAID : BillStatus.APPROVED);
        bill.setSubtotal(new BigDecimal(total));
        bill.setTotal(new BigDecimal(total));
        bill.setAmountPaid(paid);
        bill.setCreatedBy("tester");
        bill.setCreatedAt(Instant.now());
        return billRepository.save(bill);
    }

    private void mappedAccount(String code, AccountType type, AccountMappingKey key) {
        Account account = new Account();
        account.setCode(code);
        account.setName("Account " + code);
        account.setType(type);
        account.setActive(true);
        Account saved = accountRepository.save(account);
        mappingRepository.save(key, saved.getId());
        accountIds.put(key, saved.getId());
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
}
