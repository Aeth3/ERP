package com.maiu.erp.modules.finance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.maiu.erp.modules.finance.application.port.ProjectBillingReader;
import com.maiu.erp.modules.finance.application.port.SalesOrderReader;
import com.maiu.erp.modules.finance.application.service.ArInvoiceService.CreateInvoiceRequest;
import com.maiu.erp.modules.finance.application.service.ArInvoiceService.InvoiceableLine;
import com.maiu.erp.modules.finance.application.service.ArInvoiceService.LineReq;
import com.maiu.erp.modules.finance.application.service.ArInvoiceService.ProjectBillingSummary;
import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;
import com.maiu.erp.modules.finance.domain.enums.AccountType;
import com.maiu.erp.modules.finance.domain.enums.InvoiceKind;
import com.maiu.erp.modules.finance.domain.enums.InvoiceStatus;
import com.maiu.erp.modules.finance.domain.model.Account;
import com.maiu.erp.modules.finance.domain.model.Invoice;
import com.maiu.erp.modules.finance.testsupport.InMemoryAccountMappingRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryAccountRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryFiscalPeriodRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryInvoiceRepository;
import com.maiu.erp.modules.finance.testsupport.InMemoryJournalEntryRepository;
import com.maiu.erp.shared.exception.BadRequestException;

class ArInvoiceServiceTest {
    private static final LocalDate INVOICE_DATE = LocalDate.of(2026, 11, 5);

    InMemoryInvoiceRepository invoiceRepository;
    InMemoryAccountRepository accountRepository;
    InMemoryAccountMappingRepository mappingRepository;
    InMemoryJournalEntryRepository journalEntryRepository;
    StubSalesOrderReader salesOrderReader;
    StubProjectBillingReader projectReader;
    ArInvoiceService service;
    final Map<AccountMappingKey, UUID> accountIds = new HashMap<>();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("ar-clerk", "n/a", "ROLE_FINANCE"));
        invoiceRepository = new InMemoryInvoiceRepository();
        accountRepository = new InMemoryAccountRepository();
        mappingRepository = new InMemoryAccountMappingRepository();
        journalEntryRepository = new InMemoryJournalEntryRepository();
        salesOrderReader = new StubSalesOrderReader();
        projectReader = new StubProjectBillingReader();

        mapAccount(AccountMappingKey.ACCOUNTS_RECEIVABLE, "1100", AccountType.ASSET);
        mapAccount(AccountMappingKey.RETENTION_RECEIVABLE, "1110", AccountType.ASSET);
        mapAccount(AccountMappingKey.SALES_REVENUE, "4100", AccountType.REVENUE);
        mapAccount(AccountMappingKey.CONTRACT_REVENUE, "4000", AccountType.REVENUE);

        LedgerPostingService ledgerPostingService = new LedgerPostingService(
                accountRepository, journalEntryRepository,
                new FiscalPeriodService(new InMemoryFiscalPeriodRepository()));
        service = new ArInvoiceService(
                invoiceRepository, salesOrderReader, projectReader, ledgerPostingService, mappingRepository,
                "Asia/Manila");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    void mapAccount(AccountMappingKey key, String code, AccountType type) {
        Account account = new Account();
        account.setCode(code);
        account.setName(code);
        account.setType(type);
        account.setActive(true);
        account = accountRepository.save(account);
        mappingRepository.save(key, account.getId());
        accountIds.put(key, account.getId());
    }

    // --- fixtures ---

    record SoFixture(UUID soId, UUID customerId, UUID itemId, UUID productId) {
    }

    SoFixture shippedSo(String quantity, String unitPrice) {
        UUID soId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        salesOrderReader.put(new SalesOrderReader.SoSnapshot(soId, "SO-1", customerId, "SHIPPED",
                new ArrayList<>(List.of(new SalesOrderReader.SoLine(
                        itemId, productId, "Rebar 10mm",
                        new BigDecimal(quantity), BigDecimal.ZERO, new BigDecimal(unitPrice))))));
        return new SoFixture(soId, customerId, itemId, productId);
    }

    record ProjectFixture(UUID projectId, UUID customerId) {
    }

    ProjectFixture activeProject(String contractAmount, String retentionRate) {
        UUID projectId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        projectReader.put(new ProjectBillingReader.ProjectSnapshot(
                projectId, "PRJ-7", "Warehouse", customerId, "ACTIVE",
                contractAmount == null ? null : new BigDecimal(contractAmount),
                retentionRate == null ? null : new BigDecimal(retentionRate)));
        return new ProjectFixture(projectId, customerId);
    }

    CreateInvoiceRequest soRequest(SoFixture so, List<LineReq> lines) {
        return new CreateInvoiceRequest(
                InvoiceKind.SALES_ORDER, so.customerId(), so.soId(), null, INVOICE_DATE, null, null, lines);
    }

    CreateInvoiceRequest progressRequest(ProjectFixture project, String amount) {
        return new CreateInvoiceRequest(
                InvoiceKind.PROGRESS_BILLING, project.customerId(), null, project.projectId(), INVOICE_DATE, null,
                null, List.of(new LineReq(null, null, null, new BigDecimal(amount), "Progress billing #1")));
    }

    LineReq soLine(UUID soItemId, String quantity) {
        return new LineReq(soItemId, new BigDecimal(quantity), null, null, null);
    }

    // --- drafts ---

    @Test
    void createsSalesOrderDraftCopyingProductAndPrice() {
        SoFixture so = shippedSo("10.00", "25.00");

        Invoice invoice = service.createDraft(soRequest(so, List.of(soLine(so.itemId(), "4.00"))));

        assertEquals(InvoiceStatus.DRAFT, invoice.getStatus());
        assertEquals("ARI-202611-0001", invoice.getInvoiceNumber());
        assertEquals(so.productId(), invoice.getLines().get(0).getProductId());
        assertEquals(new BigDecimal("25.00"), invoice.getLines().get(0).getUnitPrice());
        assertEquals(new BigDecimal("100.00"), invoice.getSubtotal());
        assertEquals(new BigDecimal("100.00"), invoice.getTotal());
        assertEquals(BigDecimal.ZERO, invoice.getRetentionAmount());
        assertEquals("ar-clerk", invoice.getCreatedBy());
    }

    @Test
    void rejectsLineNotOnSalesOrder() {
        SoFixture so = shippedSo("10.00", "25.00");

        assertThrows(BadRequestException.class,
                () -> service.createDraft(soRequest(so, List.of(soLine(UUID.randomUUID(), "1.00")))));
    }

    @Test
    void rejectsCustomerMismatch() {
        SoFixture so = shippedSo("10.00", "25.00");
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                InvoiceKind.SALES_ORDER, UUID.randomUUID(), so.soId(), null, INVOICE_DATE, null, null,
                List.of(soLine(so.itemId(), "1.00")));

        assertThrows(BadRequestException.class, () -> service.createDraft(request));

        ProjectFixture project = activeProject("1000.00", "10.00");
        CreateInvoiceRequest billing = new CreateInvoiceRequest(
                InvoiceKind.PROGRESS_BILLING, UUID.randomUUID(), null, project.projectId(), INVOICE_DATE, null,
                null, List.of(new LineReq(null, null, null, new BigDecimal("100.00"), "billing")));
        assertThrows(BadRequestException.class, () -> service.createDraft(billing));
    }

    @Test
    void rejectsWrongKindIds() {
        SoFixture so = shippedSo("10.00", "25.00");
        ProjectFixture project = activeProject("1000.00", "10.00");

        CreateInvoiceRequest progressWithSo = new CreateInvoiceRequest(
                InvoiceKind.PROGRESS_BILLING, project.customerId(), so.soId(), project.projectId(), INVOICE_DATE,
                null, null, List.of(new LineReq(null, null, null, new BigDecimal("100.00"), "billing")));
        assertThrows(BadRequestException.class, () -> service.createDraft(progressWithSo));

        CreateInvoiceRequest soWithProject = new CreateInvoiceRequest(
                InvoiceKind.SALES_ORDER, so.customerId(), so.soId(), project.projectId(), INVOICE_DATE, null, null,
                List.of(soLine(so.itemId(), "1.00")));
        assertThrows(BadRequestException.class, () -> service.createDraft(soWithProject));
    }

    @Test
    void rejectsThirdDecimalPlace() {
        SoFixture so = shippedSo("10.00", "25.00");

        assertThrows(BadRequestException.class,
                () -> service.createDraft(soRequest(so, List.of(soLine(so.itemId(), "1.005")))));

        ProjectFixture project = activeProject("10000.00", "10.00");
        assertThrows(BadRequestException.class,
                () -> service.createDraft(progressRequest(project, "1000.555")));
    }

    @Test
    void computesRetentionFromRate() {
        ProjectFixture project = activeProject("10000.00", "10.00");

        Invoice invoice = service.createDraft(progressRequest(project, "1000.55"));

        assertEquals(new BigDecimal("1000.55"), invoice.getSubtotal());
        assertEquals(new BigDecimal("100.06"), invoice.getRetentionAmount());
        assertEquals(new BigDecimal("900.49"), invoice.getTotal());
    }

    @Test
    void nullRetentionRateMeansZero() {
        ProjectFixture project = activeProject("10000.00", null);

        Invoice invoice = service.createDraft(progressRequest(project, "500.00"));

        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getRetentionAmount()));
        assertEquals(new BigDecimal("500.00"), invoice.getTotal());
    }

    @Test
    void rejectsProgressBillingOnProjectWithoutContract() {
        ProjectFixture project = activeProject(null, "10.00");

        assertThrows(BadRequestException.class, () -> service.createDraft(progressRequest(project, "100.00")));
    }

    @Test
    void rejectsRetentionReleaseKindOnCreate() {
        ProjectFixture project = activeProject("10000.00", "10.00");
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                InvoiceKind.RETENTION_RELEASE, project.customerId(), null, project.projectId(), INVOICE_DATE, null,
                null, List.of(new LineReq(null, null, null, new BigDecimal("100.00"), "release")));

        assertThrows(BadRequestException.class, () -> service.createDraft(request));
    }

    @Test
    void updateDraftOnlyWhileDraft() {
        SoFixture so = shippedSo("10.00", "25.00");
        Invoice invoice = service.createDraft(soRequest(so, List.of(soLine(so.itemId(), "4.00"))));

        Invoice updated = service.updateDraft(invoice.getId(), soRequest(so, List.of(soLine(so.itemId(), "6.00"))));
        assertEquals(new BigDecimal("150.00"), updated.getTotal());
        assertEquals("ARI-202611-0001", updated.getInvoiceNumber());

        updated.setStatus(InvoiceStatus.APPROVED);
        invoiceRepository.save(updated);
        assertThrows(BadRequestException.class,
                () -> service.updateDraft(invoice.getId(), soRequest(so, List.of(soLine(so.itemId(), "2.00")))));
    }

    @Test
    void invoiceNumbersSkipTakenValues() {
        SoFixture so = shippedSo("10.00", "25.00");
        Invoice first = service.createDraft(soRequest(so, List.of(soLine(so.itemId(), "1.00"))));
        assertEquals("ARI-202611-0001", first.getInvoiceNumber());

        Invoice squatter = new Invoice();
        squatter.setInvoiceNumber("ARI-202611-0002");
        squatter.setKind(InvoiceKind.SALES_ORDER);
        squatter.setCustomerId(UUID.randomUUID());
        squatter.setStatus(InvoiceStatus.DRAFT);
        squatter.setInvoiceDate(INVOICE_DATE);
        squatter.setSubtotal(BigDecimal.ONE);
        squatter.setRetentionAmount(BigDecimal.ZERO);
        squatter.setTotal(BigDecimal.ONE);
        squatter.setAmountReceived(BigDecimal.ZERO);
        invoiceRepository.save(squatter);

        Invoice next = service.createDraft(soRequest(so, List.of(soLine(so.itemId(), "1.00"))));
        assertEquals("ARI-202611-0003", next.getInvoiceNumber());
    }

    @Test
    void invoiceableLinesSubtractsReturnsAndPriorInvoices() {
        SoFixture so = shippedSo("10.00", "25.00");
        salesOrderReader.setReturnedQuantity(so.soId(), so.itemId(), new BigDecimal("2.00"));

        Invoice prior = service.createDraft(soRequest(so, List.of(soLine(so.itemId(), "3.00"))));
        Invoice cancelled = service.createDraft(soRequest(so, List.of(soLine(so.itemId(), "4.00"))));
        cancelled.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(cancelled);

        List<InvoiceableLine> lines = service.invoiceableLines(so.soId());

        assertEquals(1, lines.size());
        InvoiceableLine line = lines.get(0);
        assertEquals(so.itemId(), line.soItemId());
        assertEquals("Rebar 10mm", line.productName());
        assertEquals(new BigDecimal("10.00"), line.shippedQuantity());
        assertEquals(new BigDecimal("2.00"), line.returnedQuantity());
        assertEquals(new BigDecimal("3.00"), line.invoicedQuantity());
        assertEquals(new BigDecimal("5.00"), line.remainingQuantity());
        assertEquals(new BigDecimal("25.00"), line.unitPrice());
        assertTrue(prior.getStatus() == InvoiceStatus.DRAFT);
    }

    @Test
    void projectBillingSummaryAggregates() {
        ProjectFixture project = activeProject("10000.00", "10.00");

        service.createDraft(progressRequest(project, "1000.00"));
        Invoice cancelled = service.createDraft(progressRequest(project, "700.00"));
        cancelled.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(cancelled);

        Invoice release = new Invoice();
        release.setInvoiceNumber("ARI-202611-0099");
        release.setKind(InvoiceKind.RETENTION_RELEASE);
        release.setCustomerId(project.customerId());
        release.setProjectId(project.projectId());
        release.setInvoiceDate(INVOICE_DATE);
        release.setStatus(InvoiceStatus.APPROVED);
        release.setSubtotal(new BigDecimal("40.00"));
        release.setRetentionAmount(BigDecimal.ZERO);
        release.setTotal(new BigDecimal("40.00"));
        release.setAmountReceived(BigDecimal.ZERO);
        invoiceRepository.save(release);

        ProjectBillingSummary summary = service.projectBillingSummary(project.projectId());

        assertEquals("PRJ-7", summary.projectCode());
        assertEquals(new BigDecimal("10000.00"), summary.contractAmount());
        assertEquals(new BigDecimal("10.00"), summary.retentionRate());
        assertEquals(new BigDecimal("1000.00"), summary.billedToDate());
        assertEquals(new BigDecimal("100.00"), summary.retentionHeld());
        assertEquals(new BigDecimal("40.00"), summary.retentionReleased());
        assertEquals(new BigDecimal("9000.00"), summary.remainingContract());
    }

    // --- stubs ---

    static final class StubSalesOrderReader implements SalesOrderReader {
        private final Map<UUID, SoSnapshot> snapshots = new HashMap<>();

        void put(SoSnapshot snapshot) {
            snapshots.put(snapshot.id(), snapshot);
        }

        void setStatus(UUID soId, String status) {
            SoSnapshot snapshot = snapshots.get(soId);
            snapshots.put(soId, new SoSnapshot(
                    snapshot.id(), snapshot.soNumber(), snapshot.customerId(), status, snapshot.lines()));
        }

        void setReturnedQuantity(UUID soId, UUID soItemId, BigDecimal returned) {
            SoSnapshot snapshot = snapshots.get(soId);
            List<SoLine> lines = new ArrayList<>();
            for (SoLine line : snapshot.lines()) {
                lines.add(line.soItemId().equals(soItemId)
                        ? new SoLine(line.soItemId(), line.productId(), line.productName(), line.quantity(),
                                returned, line.unitPrice())
                        : line);
            }
            snapshots.put(soId, new SoSnapshot(
                    snapshot.id(), snapshot.soNumber(), snapshot.customerId(), snapshot.status(), lines));
        }

        @Override
        public Optional<SoSnapshot> find(UUID salesOrderId) {
            return Optional.ofNullable(snapshots.get(salesOrderId));
        }
    }

    static final class StubProjectBillingReader implements ProjectBillingReader {
        private final Map<UUID, ProjectSnapshot> snapshots = new HashMap<>();

        void put(ProjectSnapshot snapshot) {
            snapshots.put(snapshot.id(), snapshot);
        }

        void setStatus(UUID projectId, String status) {
            ProjectSnapshot snapshot = snapshots.get(projectId);
            snapshots.put(projectId, new ProjectSnapshot(
                    snapshot.id(), snapshot.projectCode(), snapshot.projectName(), snapshot.customerId(), status,
                    snapshot.contractAmount(), snapshot.retentionRate()));
        }

        @Override
        public Optional<ProjectSnapshot> find(UUID projectId) {
            return Optional.ofNullable(snapshots.get(projectId));
        }
    }
}
