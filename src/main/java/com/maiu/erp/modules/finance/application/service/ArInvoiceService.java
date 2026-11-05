package com.maiu.erp.modules.finance.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.finance.application.port.ProjectBillingReader;
import com.maiu.erp.modules.finance.application.port.ProjectBillingReader.ProjectSnapshot;
import com.maiu.erp.modules.finance.application.port.SalesOrderReader;
import com.maiu.erp.modules.finance.application.port.SalesOrderReader.SoLine;
import com.maiu.erp.modules.finance.application.port.SalesOrderReader.SoSnapshot;
import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;
import com.maiu.erp.modules.finance.domain.enums.InvoiceKind;
import com.maiu.erp.modules.finance.domain.enums.InvoiceStatus;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;
import com.maiu.erp.modules.finance.domain.model.Invoice;
import com.maiu.erp.modules.finance.domain.model.InvoiceLine;
import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.model.JournalLine;
import com.maiu.erp.modules.finance.domain.repository.AccountMappingRepository;
import com.maiu.erp.modules.finance.domain.repository.InvoiceRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;

/**
 * Customer invoices: sales-order invoices matched against shipped quantities net of returns,
 * project progress billings capped at the contract with retention withheld, and retention
 * releases. Approval posts to the ledger in the same transaction.
 */
@Service
public class ArInvoiceService {
    private static final int MONEY_SCALE = 2;
    private static final DateTimeFormatter NUMBER_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final InvoiceRepository invoiceRepository;
    private final SalesOrderReader salesOrderReader;
    private final ProjectBillingReader projectBillingReader;
    private final LedgerPostingService ledgerPostingService;
    private final AccountMappingRepository accountMappingRepository;
    private final ZoneId businessZone;

    public ArInvoiceService(
            InvoiceRepository invoiceRepository,
            SalesOrderReader salesOrderReader,
            ProjectBillingReader projectBillingReader,
            LedgerPostingService ledgerPostingService,
            AccountMappingRepository accountMappingRepository,
            @Value("${finance.time-zone:Asia/Manila}") String businessZone) {
        this.invoiceRepository = invoiceRepository;
        this.salesOrderReader = salesOrderReader;
        this.projectBillingReader = projectBillingReader;
        this.ledgerPostingService = ledgerPostingService;
        this.accountMappingRepository = accountMappingRepository;
        this.businessZone = ZoneId.of(businessZone);
    }

    public record CreateInvoiceRequest(
            InvoiceKind kind,
            UUID customerId,
            UUID salesOrderId,
            UUID projectId,
            LocalDate invoiceDate,
            LocalDate dueDate,
            String notes,
            List<LineReq> lines) {
    }

    public record LineReq(
            UUID salesOrderItemId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            String description) {
    }

    @Transactional
    public Invoice createDraft(CreateInvoiceRequest request) {
        validateRequest(request);
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setCreatedBy(CurrentActor.name());
        invoice.setCreatedAt(Instant.now());
        invoice.setAmountReceived(BigDecimal.ZERO);
        applyRequest(invoice, request);
        invoice.setInvoiceNumber(nextInvoiceNumber(request.invoiceDate()));
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice updateDraft(UUID id, CreateInvoiceRequest request) {
        Invoice invoice = getForUpdate(id);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BadRequestException("Only draft invoices can be edited");
        }
        validateRequest(request);
        applyRequest(invoice, request);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice approve(UUID id) {
        Invoice invoice = getForUpdate(id);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BadRequestException("Only draft invoices can be approved");
        }

        switch (invoice.getKind()) {
            case SALES_ORDER -> matchAgainstSalesOrder(invoice);
            case PROGRESS_BILLING -> matchAgainstContract(invoice);
            case RETENTION_RELEASE -> matchAgainstReleasable(invoice);
        }

        JournalEntry posted = ledgerPostingService.post(entryFor(invoice));
        invoice.setJournalEntryId(posted.getId());
        invoice.setStatus(InvoiceStatus.APPROVED);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice cancel(UUID id) {
        Invoice invoice = getForUpdate(id);
        switch (invoice.getStatus()) {
            case DRAFT -> invoice.setStatus(InvoiceStatus.CANCELLED);
            case APPROVED -> {
                if (invoice.getAmountReceived() != null && invoice.getAmountReceived().signum() != 0) {
                    throw new BadRequestException(
                            "Invoice " + invoice.getInvoiceNumber() + " has receipts and cannot be cancelled");
                }
                if (invoice.getKind() == InvoiceKind.PROGRESS_BILLING
                        && releasableRetention(invoice.getProjectId(), invoice.getId())
                                .compareTo(BigDecimal.ZERO) < 0) {
                    throw new BadRequestException(
                            "Invoice " + invoice.getInvoiceNumber()
                                    + " has retention that was already released; cancel the retention release first");
                }
                ledgerPostingService.reverse(
                        invoice.getJournalEntryId(),
                        LocalDate.now(businessZone),
                        "Cancel invoice " + invoice.getInvoiceNumber(),
                        CurrentActor.name());
                invoice.setStatus(InvoiceStatus.CANCELLED);
            }
            case CANCELLED -> throw new BadRequestException(
                    "Invoice " + invoice.getInvoiceNumber() + " is already cancelled");
            default -> throw new BadRequestException(
                    "Invoice " + invoice.getInvoiceNumber() + " has receipts and cannot be cancelled");
        }
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice releaseRetention(UUID projectId, LocalDate invoiceDate, LocalDate dueDate) {
        ProjectSnapshot snapshot = projectBillingReader.find(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        if (invoiceDate == null) {
            throw new BadRequestException("Invoice date is required");
        }
        BigDecimal releasable = releasableRetention(projectId, null);
        if (releasable.signum() <= 0) {
            throw new BadRequestException(
                    "Project " + snapshot.projectCode() + " has no retention left to release");
        }

        Invoice invoice = new Invoice();
        invoice.setKind(InvoiceKind.RETENTION_RELEASE);
        invoice.setCustomerId(snapshot.customerId());
        invoice.setProjectId(projectId);
        invoice.setInvoiceDate(invoiceDate);
        invoice.setDueDate(dueDate);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setSubtotal(releasable);
        invoice.setRetentionAmount(BigDecimal.ZERO);
        invoice.setTotal(releasable);
        invoice.setAmountReceived(BigDecimal.ZERO);
        invoice.setCreatedBy(CurrentActor.name());
        invoice.setCreatedAt(Instant.now());
        InvoiceLine line = new InvoiceLine();
        line.setLineNumber(1);
        line.setDescription("Retention release — " + snapshot.projectCode());
        line.setLineTotal(releasable);
        invoice.setLines(List.of(line));
        invoice.setInvoiceNumber(nextInvoiceNumber(invoiceDate));
        return invoiceRepository.save(invoice);
    }

    // --- matching ---

    private void matchAgainstSalesOrder(Invoice invoice) {
        SoSnapshot snapshot = salesOrderReader.find(invoice.getSalesOrderId())
                .orElseThrow(() -> new NotFoundException("Sales order not found"));
        if (!"SHIPPED".equals(snapshot.status()) && !"DELIVERED".equals(snapshot.status())) {
            throw new BadRequestException(
                    "Sales order " + snapshot.soNumber() + " has not been shipped");
        }
        if (!snapshot.customerId().equals(invoice.getCustomerId())) {
            throw new BadRequestException("Invoice customer does not match the sales order customer");
        }

        Map<UUID, SoLine> soLines = soLinesByItemId(snapshot);
        Map<UUID, BigDecimal> alreadyInvoiced = invoicedQuantities(invoice.getSalesOrderId(), invoice.getId());
        for (InvoiceLine line : invoice.getLines()) {
            SoLine soLine = soLines.get(line.getSalesOrderItemId());
            if (soLine == null) {
                throw new BadRequestException("Invoice line does not reference an item of the sales order");
            }
            BigDecimal invoiceable = soLine.quantity().subtract(soLine.returnedQuantity());
            // merge() keeps repeated lines for the same SO item cumulative within this invoice too.
            BigDecimal cumulative = alreadyInvoiced.merge(
                    line.getSalesOrderItemId(), line.getQuantity(), BigDecimal::add);
            if (cumulative.compareTo(invoiceable) > 0) {
                throw new BadRequestException(
                        "Invoiced quantity would exceed the shipped quantity (net of returns) for "
                                + soLine.productName());
            }
        }
    }

    private void matchAgainstContract(Invoice invoice) {
        ProjectSnapshot snapshot = projectBillingReader.find(invoice.getProjectId())
                .orElseThrow(() -> new NotFoundException("Project not found"));
        if (snapshot.contractAmount() == null) {
            throw new BadRequestException(
                    "Project " + snapshot.projectCode() + " has no contract amount to bill against");
        }
        BigDecimal billed = BigDecimal.ZERO;
        for (Invoice other : invoiceRepository.findByProjectId(invoice.getProjectId())) {
            if (other.getStatus() == InvoiceStatus.CANCELLED
                    || other.getId().equals(invoice.getId())
                    || other.getKind() != InvoiceKind.PROGRESS_BILLING) {
                continue;
            }
            billed = billed.add(other.getSubtotal());
        }
        if (billed.add(invoice.getSubtotal()).compareTo(snapshot.contractAmount()) > 0) {
            throw new BadRequestException(
                    "Billing would exceed the contract amount for project " + snapshot.projectCode());
        }
    }

    private void matchAgainstReleasable(Invoice invoice) {
        BigDecimal releasable = releasableRetention(invoice.getProjectId(), invoice.getId());
        if (invoice.getSubtotal().compareTo(releasable) > 0) {
            throw new BadRequestException("Release would exceed the retention held for the project");
        }
    }

    /** Retention held on approved billings minus already-released amounts, excluding one invoice if given. */
    private BigDecimal releasableRetention(UUID projectId, UUID excludedInvoiceId) {
        BigDecimal held = BigDecimal.ZERO;
        BigDecimal released = BigDecimal.ZERO;
        for (Invoice invoice : invoiceRepository.findByProjectId(projectId)) {
            if (invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getId().equals(excludedInvoiceId)) {
                continue;
            }
            if (invoice.getKind() == InvoiceKind.PROGRESS_BILLING && invoice.getStatus() != InvoiceStatus.DRAFT) {
                held = held.add(invoice.getRetentionAmount());
            } else if (invoice.getKind() == InvoiceKind.RETENTION_RELEASE && invoice.getStatus() != InvoiceStatus.DRAFT) {
                released = released.add(invoice.getSubtotal());
            }
        }
        return held.subtract(released);
    }

    // --- posting ---

    private JournalEntry entryFor(Invoice invoice) {
        List<JournalLine> lines = new ArrayList<>();
        switch (invoice.getKind()) {
            case SALES_ORDER -> {
                addNonZeroLine(lines, AccountMappingKey.ACCOUNTS_RECEIVABLE, invoice.getTotal(), null);
                addNonZeroLine(lines, AccountMappingKey.SALES_REVENUE, null, invoice.getTotal());
            }
            case PROGRESS_BILLING -> {
                addNonZeroLine(lines, AccountMappingKey.ACCOUNTS_RECEIVABLE, invoice.getTotal(), null);
                addNonZeroLine(lines, AccountMappingKey.RETENTION_RECEIVABLE, invoice.getRetentionAmount(), null);
                addNonZeroLine(lines, AccountMappingKey.CONTRACT_REVENUE, null, invoice.getSubtotal());
            }
            case RETENTION_RELEASE -> {
                addNonZeroLine(lines, AccountMappingKey.ACCOUNTS_RECEIVABLE, invoice.getTotal(), null);
                addNonZeroLine(lines, AccountMappingKey.RETENTION_RECEIVABLE, null, invoice.getTotal());
            }
        }
        if (lines.isEmpty()) {
            throw new BadRequestException(
                    "Invoice " + invoice.getInvoiceNumber() + " has a zero total and cannot be posted");
        }

        JournalEntry draft = new JournalEntry();
        draft.setEntryDate(invoice.getInvoiceDate());
        draft.setSourceType(JournalSourceType.AR_INVOICE);
        draft.setSourceId(invoice.getId());
        draft.setMemo("Invoice " + invoice.getInvoiceNumber());
        draft.setCreatedBy(CurrentActor.name());
        draft.setLines(lines);
        return draft;
    }

    /** Zero-amount journal lines are omitted; a null or zero debit/credit adds nothing. */
    private void addNonZeroLine(List<JournalLine> lines, AccountMappingKey key, BigDecimal debit, BigDecimal credit) {
        boolean hasDebit = debit != null && debit.signum() > 0;
        boolean hasCredit = credit != null && credit.signum() > 0;
        if (!hasDebit && !hasCredit) {
            return;
        }
        lines.add(journalLine(mappedAccount(key), hasDebit ? debit : null, hasCredit ? credit : null));
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

    public Invoice get(UUID id) {
        return invoiceRepository.findById(id).orElseThrow(() -> new NotFoundException("Invoice not found"));
    }

    public List<Invoice> search(UUID customerId, InvoiceStatus status, InvoiceKind kind) {
        return invoiceRepository.search(customerId, status, kind);
    }

    public BigDecimal remainingDue(Invoice invoice) {
        BigDecimal received = invoice.getAmountReceived() == null ? BigDecimal.ZERO : invoice.getAmountReceived();
        return invoice.getTotal().subtract(received);
    }

    public record InvoiceableLine(
            UUID soItemId,
            UUID productId,
            String productName,
            BigDecimal shippedQuantity,
            BigDecimal returnedQuantity,
            BigDecimal invoicedQuantity,
            BigDecimal remainingQuantity,
            BigDecimal unitPrice) {
    }

    /** What is still invoiceable on a sales order: ordered minus returned minus already invoiced, per item. */
    public List<InvoiceableLine> invoiceableLines(UUID salesOrderId) {
        SoSnapshot snapshot = salesOrderReader.find(salesOrderId)
                .orElseThrow(() -> new NotFoundException("Sales order not found"));

        Map<UUID, BigDecimal> invoiced = invoicedQuantities(salesOrderId, null);

        List<InvoiceableLine> lines = new ArrayList<>();
        for (SoLine soLine : snapshot.lines()) {
            BigDecimal invoicedQuantity = invoiced.getOrDefault(soLine.soItemId(), BigDecimal.ZERO);
            BigDecimal remaining = soLine.quantity().subtract(soLine.returnedQuantity()).subtract(invoicedQuantity);
            lines.add(new InvoiceableLine(
                    soLine.soItemId(),
                    soLine.productId(),
                    soLine.productName(),
                    soLine.quantity(),
                    soLine.returnedQuantity(),
                    invoicedQuantity,
                    remaining.max(BigDecimal.ZERO),
                    soLine.unitPrice()));
        }
        return lines;
    }

    public record ProjectBillingSummary(
            UUID projectId,
            String projectCode,
            BigDecimal contractAmount,
            BigDecimal retentionRate,
            BigDecimal billedToDate,
            BigDecimal retentionHeld,
            BigDecimal retentionReleased,
            BigDecimal remainingContract) {
    }

    public ProjectBillingSummary projectBillingSummary(UUID projectId) {
        ProjectSnapshot snapshot = projectBillingReader.find(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        BigDecimal billedToDate = BigDecimal.ZERO;
        BigDecimal retentionHeld = BigDecimal.ZERO;
        BigDecimal retentionReleased = BigDecimal.ZERO;
        for (Invoice invoice : invoiceRepository.findByProjectId(projectId)) {
            if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
                continue;
            }
            if (invoice.getKind() == InvoiceKind.PROGRESS_BILLING) {
                billedToDate = billedToDate.add(invoice.getSubtotal());
                retentionHeld = retentionHeld.add(invoice.getRetentionAmount());
            } else if (invoice.getKind() == InvoiceKind.RETENTION_RELEASE) {
                retentionReleased = retentionReleased.add(invoice.getSubtotal());
            }
        }
        BigDecimal contract = snapshot.contractAmount();
        BigDecimal remaining = contract == null ? null : contract.subtract(billedToDate);
        return new ProjectBillingSummary(
                projectId,
                snapshot.projectCode(),
                contract,
                snapshot.retentionRate(),
                billedToDate,
                retentionHeld,
                retentionReleased,
                remaining);
    }

    // --- shared internals ---

    Invoice getForUpdate(UUID id) {
        return invoiceRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Invoice not found"));
    }

    /** Quantities invoiced per SO item on non-cancelled invoices, excluding one invoice if given. */
    Map<UUID, BigDecimal> invoicedQuantities(UUID salesOrderId, UUID excludedInvoiceId) {
        Map<UUID, BigDecimal> invoiced = new HashMap<>();
        for (Invoice other : invoiceRepository.findBySalesOrderId(salesOrderId)) {
            if (other.getStatus() == InvoiceStatus.CANCELLED || other.getId().equals(excludedInvoiceId)) {
                continue;
            }
            for (InvoiceLine line : other.getLines()) {
                invoiced.merge(line.getSalesOrderItemId(), line.getQuantity(), BigDecimal::add);
            }
        }
        return invoiced;
    }

    private void validateRequest(CreateInvoiceRequest request) {
        if (request.kind() == null) {
            throw new BadRequestException("Invoice kind is required");
        }
        if (request.kind() == InvoiceKind.RETENTION_RELEASE) {
            throw new BadRequestException("Retention release invoices are created via the release action");
        }
        if (request.customerId() == null) {
            throw new BadRequestException("Customer is required");
        }
        if (request.invoiceDate() == null) {
            throw new BadRequestException("Invoice date is required");
        }
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new BadRequestException("An invoice needs at least one line");
        }

        if (request.kind() == InvoiceKind.SALES_ORDER) {
            if (request.salesOrderId() == null || request.projectId() != null) {
                throw new BadRequestException("A sales order invoice references a sales order and no project");
            }
            SoSnapshot snapshot = salesOrderReader.find(request.salesOrderId())
                    .orElseThrow(() -> new NotFoundException("Sales order not found"));
            if (!snapshot.customerId().equals(request.customerId())) {
                throw new BadRequestException("Invoice customer does not match the sales order customer");
            }
            Map<UUID, SoLine> soLines = soLinesByItemId(snapshot);
            for (LineReq line : request.lines()) {
                if (line.salesOrderItemId() == null || line.amount() != null) {
                    throw new BadRequestException("Sales order invoice lines must reference sales order items");
                }
                if (!soLines.containsKey(line.salesOrderItemId())) {
                    throw new BadRequestException("Invoice line does not reference an item of the sales order");
                }
                if (line.quantity() == null || line.quantity().signum() <= 0) {
                    throw new BadRequestException("Line quantity must be positive");
                }
                requireMoneyScale(line.quantity(), "quantity");
                if (line.unitPrice() != null) {
                    if (line.unitPrice().signum() < 0) {
                        throw new BadRequestException("Line unit price must not be negative");
                    }
                    requireMoneyScale(line.unitPrice(), "unit price");
                }
            }
        } else {
            if (request.projectId() == null || request.salesOrderId() != null) {
                throw new BadRequestException("A progress billing references a project and no sales order");
            }
            ProjectSnapshot snapshot = projectBillingReader.find(request.projectId())
                    .orElseThrow(() -> new NotFoundException("Project not found"));
            if (!snapshot.customerId().equals(request.customerId())) {
                throw new BadRequestException("Invoice customer does not match the project customer");
            }
            if (!"ACTIVE".equals(snapshot.status())) {
                throw new BadRequestException(
                        "Project " + snapshot.projectCode() + " is not active and cannot be billed");
            }
            if (snapshot.contractAmount() == null) {
                throw new BadRequestException(
                        "Project " + snapshot.projectCode() + " has no contract amount to bill against");
            }
            if (request.lines().size() != 1) {
                throw new BadRequestException("A progress billing has exactly one line");
            }
            LineReq line = request.lines().get(0);
            if (line.salesOrderItemId() != null || line.quantity() != null || line.unitPrice() != null) {
                throw new BadRequestException("A progress billing line carries only a description and an amount");
            }
            if (line.amount() == null || line.amount().signum() <= 0) {
                throw new BadRequestException("Billing amount must be positive");
            }
            if (line.description() == null || line.description().isBlank()) {
                throw new BadRequestException("A progress billing line needs a description");
            }
            requireMoneyScale(line.amount(), "amount");
        }
    }

    private void applyRequest(Invoice invoice, CreateInvoiceRequest request) {
        invoice.setKind(request.kind());
        invoice.setCustomerId(request.customerId());
        invoice.setSalesOrderId(request.salesOrderId());
        invoice.setProjectId(request.projectId());
        invoice.setInvoiceDate(request.invoiceDate());
        invoice.setDueDate(request.dueDate());
        invoice.setNotes(request.notes());

        List<InvoiceLine> lines = new ArrayList<>();
        int lineNumber = 1;
        if (request.kind() == InvoiceKind.SALES_ORDER) {
            SoSnapshot snapshot = salesOrderReader.find(request.salesOrderId())
                    .orElseThrow(() -> new NotFoundException("Sales order not found"));
            Map<UUID, SoLine> soLines = soLinesByItemId(snapshot);
            BigDecimal subtotal = BigDecimal.ZERO;
            for (LineReq lineReq : request.lines()) {
                SoLine soLine = soLines.get(lineReq.salesOrderItemId());
                InvoiceLine line = new InvoiceLine();
                line.setLineNumber(lineNumber++);
                line.setSalesOrderItemId(lineReq.salesOrderItemId());
                line.setProductId(soLine.productId());
                line.setDescription(lineReq.description());
                line.setQuantity(lineReq.quantity());
                BigDecimal unitPrice = lineReq.unitPrice() != null ? lineReq.unitPrice() : soLine.unitPrice();
                line.setUnitPrice(unitPrice);
                line.setLineTotal(money(lineReq.quantity().multiply(unitPrice)));
                subtotal = subtotal.add(line.getLineTotal());
                lines.add(line);
            }
            invoice.setSubtotal(subtotal);
            invoice.setRetentionAmount(BigDecimal.ZERO);
            invoice.setTotal(subtotal);
        } else {
            ProjectSnapshot snapshot = projectBillingReader.find(request.projectId())
                    .orElseThrow(() -> new NotFoundException("Project not found"));
            LineReq lineReq = request.lines().get(0);
            InvoiceLine line = new InvoiceLine();
            line.setLineNumber(lineNumber);
            line.setDescription(lineReq.description());
            line.setLineTotal(money(lineReq.amount()));
            lines.add(line);

            BigDecimal subtotal = line.getLineTotal();
            BigDecimal rate = snapshot.retentionRate() == null ? BigDecimal.ZERO : snapshot.retentionRate();
            BigDecimal retention = money(subtotal.multiply(rate).divide(new BigDecimal("100")));
            invoice.setSubtotal(subtotal);
            invoice.setRetentionAmount(retention);
            invoice.setTotal(subtotal.subtract(retention));
        }
        invoice.setLines(lines);
    }

    private static Map<UUID, SoLine> soLinesByItemId(SoSnapshot snapshot) {
        Map<UUID, SoLine> byItemId = new HashMap<>();
        for (SoLine line : snapshot.lines()) {
            byItemId.put(line.soItemId(), line);
        }
        return byItemId;
    }

    String nextInvoiceNumber(LocalDate invoiceDate) {
        String prefix = "ARI-" + NUMBER_MONTH.format(invoiceDate) + "-";
        long sequence = invoiceRepository.search(null, null, null).stream()
                .filter(invoice -> invoice.getInvoiceNumber() != null
                        && invoice.getInvoiceNumber().startsWith(prefix))
                .count() + 1;
        // Skip numbers already taken (gaps, concurrent creates); the unique column is the backstop.
        String candidate = prefix + String.format("%04d", sequence);
        while (invoiceRepository.existsByInvoiceNumber(candidate)) {
            sequence++;
            candidate = prefix + String.format("%04d", sequence);
        }
        return candidate;
    }

    private static void requireMoneyScale(BigDecimal value, String field) {
        if (value.stripTrailingZeros().scale() > MONEY_SCALE) {
            throw new BadRequestException("Line " + field + " must have at most two decimal places");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    ZoneId businessZone() {
        return businessZone;
    }
}
