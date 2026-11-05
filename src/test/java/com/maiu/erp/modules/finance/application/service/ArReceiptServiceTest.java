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

import com.maiu.erp.modules.finance.application.service.ArReceiptService.AllocationReq;
import com.maiu.erp.modules.finance.application.service.ArReceiptService.CreateReceiptRequest;
import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;
import com.maiu.erp.modules.finance.domain.enums.AccountType;
import com.maiu.erp.modules.finance.domain.enums.InvoiceKind;
import com.maiu.erp.modules.finance.domain.enums.InvoiceStatus;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;
import com.maiu.erp.modules.finance.domain.enums.PaymentMethod;
import com.maiu.erp.modules.finance.domain.model.Account;
import com.maiu.erp.modules.finance.domain.model.Invoice;
import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.model.Receipt;
import com.maiu.erp.modules.finance.testsupport.InMemoryAccountMappingRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryAccountRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryFiscalPeriodRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryInvoiceRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryJournalEntryRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryReceiptRepository;
import com.maiu.erp.shared.exception.BadRequestException;

class ArReceiptServiceTest {
    private static final LocalDate RECEIPT_DATE = LocalDate.of(2026, 11, 10);

    private InMemoryReceiptRepository receiptRepository;
    private InMemoryInvoiceRepository invoiceRepository;
    private InMemoryJournalEntryRepository journalEntryRepository;
    private ArReceiptService service;
    private final Map<AccountMappingKey, UUID> accountIds = new HashMap<>();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("cashier", "n/a", "ROLE_FINANCE"));
        receiptRepository = new InMemoryReceiptRepository();
        invoiceRepository = new InMemoryInvoiceRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryAccountMappingRepository mappingRepository = new InMemoryAccountMappingRepository();
        journalEntryRepository = new InMemoryJournalEntryRepository();

        for (AccountMappingKey key : List.of(
                AccountMappingKey.ACCOUNTS_RECEIVABLE, AccountMappingKey.CASH, AccountMappingKey.BANK)) {
            Account account = new Account();
            account.setCode(key.name());
            account.setName(key.name());
            account.setType(AccountType.ASSET);
            account.setActive(true);
            account = accountRepository.save(account);
            mappingRepository.save(key, account.getId());
            accountIds.put(key, account.getId());
        }

        LedgerPostingService ledgerPostingService = new LedgerPostingService(
                accountRepository, journalEntryRepository,
                new FiscalPeriodService(new InMemoryFiscalPeriodRepository()));
        service = new ArReceiptService(
                receiptRepository, invoiceRepository, ledgerPostingService, mappingRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Invoice openInvoice(UUID customerId, String total) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("ARI-202611-" + String.format("%04d", invoiceRepository.search(null, null, null).size() + 1));
        invoice.setKind(InvoiceKind.SALES_ORDER);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceDate(LocalDate.of(2026, 11, 5));
        invoice.setStatus(InvoiceStatus.APPROVED);
        invoice.setSubtotal(new BigDecimal(total));
        invoice.setRetentionAmount(BigDecimal.ZERO);
        invoice.setTotal(new BigDecimal(total));
        invoice.setAmountReceived(BigDecimal.ZERO);
        invoice.setCreatedBy("tester");
        invoice.setCreatedAt(Instant.now());
        return invoiceRepository.save(invoice);
    }

    private CreateReceiptRequest request(UUID customerId, PaymentMethod method, List<AllocationReq> allocations) {
        return new CreateReceiptRequest(customerId, RECEIPT_DATE, method, null, allocations);
    }

    @Test
    void createsReceiptAndRollsUpStatus() {
        UUID customerId = UUID.randomUUID();
        Invoice invoice = openInvoice(customerId, "100.00");

        Receipt receipt = service.create(request(customerId, PaymentMethod.CASH,
                List.of(new AllocationReq(invoice.getId(), new BigDecimal("100.00")))));

        assertEquals("RCT-202611-0001", receipt.getReceiptNumber());
        assertEquals(new BigDecimal("100.00"), receipt.getTotal());
        Invoice updated = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(InvoiceStatus.PAID, updated.getStatus());
        assertEquals(new BigDecimal("100.00"), updated.getAmountReceived());

        JournalEntry entry = journalEntryRepository.findById(receipt.getJournalEntryId()).orElseThrow();
        assertEquals(JournalSourceType.AR_RECEIPT, entry.getSourceType());
        assertEquals("Receipt RCT-202611-0001", entry.getMemo());
    }

    @Test
    void rejectsAllocationBeyondRemainingDue() {
        UUID customerId = UUID.randomUUID();
        Invoice invoice = openInvoice(customerId, "100.00");

        // Two allocations of 60 to the same invoice are cumulative.
        assertThrows(BadRequestException.class, () -> service.create(request(customerId, PaymentMethod.CASH,
                List.of(new AllocationReq(invoice.getId(), new BigDecimal("60.00")),
                        new AllocationReq(invoice.getId(), new BigDecimal("60.00"))))));
    }

    @Test
    void rejectsNonOpenInvoice() {
        UUID customerId = UUID.randomUUID();
        Invoice draft = openInvoice(customerId, "100.00");
        draft.setStatus(InvoiceStatus.DRAFT);
        invoiceRepository.save(draft);
        assertThrows(BadRequestException.class, () -> service.create(request(customerId, PaymentMethod.CASH,
                List.of(new AllocationReq(draft.getId(), new BigDecimal("10.00"))))));

        Invoice cancelled = openInvoice(customerId, "100.00");
        cancelled.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(cancelled);
        assertThrows(BadRequestException.class, () -> service.create(request(customerId, PaymentMethod.CASH,
                List.of(new AllocationReq(cancelled.getId(), new BigDecimal("10.00"))))));
    }

    @Test
    void rejectsCustomerMismatch() {
        Invoice invoice = openInvoice(UUID.randomUUID(), "100.00");

        assertThrows(BadRequestException.class, () -> service.create(request(UUID.randomUUID(), PaymentMethod.CASH,
                List.of(new AllocationReq(invoice.getId(), new BigDecimal("10.00"))))));
    }

    @Test
    void postsCashVsBankByMethod() {
        UUID customerId = UUID.randomUUID();
        Invoice first = openInvoice(customerId, "100.00");
        Receipt cash = service.create(request(customerId, PaymentMethod.CASH,
                List.of(new AllocationReq(first.getId(), new BigDecimal("40.00")))));
        JournalEntry cashEntry = journalEntryRepository.findById(cash.getJournalEntryId()).orElseThrow();
        UUID cashAccount = accountIds.get(AccountMappingKey.CASH);
        UUID arAccount = accountIds.get(AccountMappingKey.ACCOUNTS_RECEIVABLE);
        assertTrue(cashEntry.getLines().stream().anyMatch(line -> cashAccount.equals(line.getAccountId())
                && new BigDecimal("40.00").equals(line.getDebit())));
        assertTrue(cashEntry.getLines().stream().anyMatch(line -> arAccount.equals(line.getAccountId())
                && new BigDecimal("40.00").equals(line.getCredit())));

        Receipt check = service.create(request(customerId, PaymentMethod.CHECK,
                List.of(new AllocationReq(first.getId(), new BigDecimal("30.00")))));
        JournalEntry checkEntry = journalEntryRepository.findById(check.getJournalEntryId()).orElseThrow();
        UUID bankAccount = accountIds.get(AccountMappingKey.BANK);
        assertTrue(checkEntry.getLines().stream().anyMatch(line -> bankAccount.equals(line.getAccountId())
                && new BigDecimal("30.00").equals(line.getDebit())));
    }

    @Test
    void locksInvoicesOnCreate() {
        UUID customerId = UUID.randomUUID();
        Invoice invoice = openInvoice(customerId, "100.00");

        int before = invoiceRepository.getForUpdateCalls();
        service.create(request(customerId, PaymentMethod.CASH,
                List.of(new AllocationReq(invoice.getId(), new BigDecimal("10.00")))));
        assertTrue(invoiceRepository.getForUpdateCalls() > before);
    }

    @Test
    void receiptNumbersSkipTakenValues() {
        UUID customerId = UUID.randomUUID();
        Invoice invoice = openInvoice(customerId, "100.00");

        Receipt squatter = new Receipt();
        squatter.setReceiptNumber("RCT-202611-0001");
        squatter.setCustomerId(customerId);
        squatter.setReceiptDate(RECEIPT_DATE);
        squatter.setMethod(PaymentMethod.CASH);
        squatter.setTotal(BigDecimal.ONE);
        squatter.setCreatedBy("tester");
        squatter.setCreatedAt(Instant.now());
        receiptRepository.save(squatter);

        Receipt receipt = service.create(request(customerId, PaymentMethod.CASH,
                List.of(new AllocationReq(invoice.getId(), new BigDecimal("10.00")))));
        assertEquals("RCT-202611-0002", receipt.getReceiptNumber());
    }

    @Test
    void partialThenFinalReceiptMarksPaid() {
        UUID customerId = UUID.randomUUID();
        Invoice invoice = openInvoice(customerId, "100.00");

        service.create(request(customerId, PaymentMethod.BANK,
                List.of(new AllocationReq(invoice.getId(), new BigDecimal("60.00")))));
        assertEquals(InvoiceStatus.PARTIALLY_PAID,
                invoiceRepository.findById(invoice.getId()).orElseThrow().getStatus());

        service.create(request(customerId, PaymentMethod.BANK,
                List.of(new AllocationReq(invoice.getId(), new BigDecimal("40.00")))));
        Invoice updated = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(InvoiceStatus.PAID, updated.getStatus());
        assertEquals(new BigDecimal("100.00"), updated.getAmountReceived());
    }
}
