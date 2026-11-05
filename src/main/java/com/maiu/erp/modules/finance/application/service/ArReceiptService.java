package com.maiu.erp.modules.finance.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.finance.domain.enums.AccountMappingKey;
import com.maiu.erp.modules.finance.domain.enums.InvoiceStatus;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;
import com.maiu.erp.modules.finance.domain.enums.PaymentMethod;
import com.maiu.erp.modules.finance.domain.model.Invoice;
import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.model.JournalLine;
import com.maiu.erp.modules.finance.domain.model.Receipt;
import com.maiu.erp.modules.finance.domain.model.ReceiptAllocation;
import com.maiu.erp.modules.finance.domain.repository.AccountMappingRepository;
import com.maiu.erp.modules.finance.domain.repository.InvoiceRepository;
import com.maiu.erp.modules.finance.domain.repository.ReceiptRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;

/**
 * Customer receipts. A receipt settles one or more open invoices of one customer;
 * posting (Dr Cash or Bank / Cr AR) happens in the same transaction as the invoice updates.
 */
@Service
public class ArReceiptService {
    private static final DateTimeFormatter NUMBER_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final ReceiptRepository receiptRepository;
    private final InvoiceRepository invoiceRepository;
    private final LedgerPostingService ledgerPostingService;
    private final AccountMappingRepository accountMappingRepository;

    public ArReceiptService(
            ReceiptRepository receiptRepository,
            InvoiceRepository invoiceRepository,
            LedgerPostingService ledgerPostingService,
            AccountMappingRepository accountMappingRepository) {
        this.receiptRepository = receiptRepository;
        this.invoiceRepository = invoiceRepository;
        this.ledgerPostingService = ledgerPostingService;
        this.accountMappingRepository = accountMappingRepository;
    }

    public record CreateReceiptRequest(
            UUID customerId,
            LocalDate receiptDate,
            PaymentMethod method,
            String reference,
            List<AllocationReq> allocations) {
    }

    public record AllocationReq(UUID invoiceId, BigDecimal amount) {
    }

    @Transactional
    public Receipt create(CreateReceiptRequest request) {
        if (request.customerId() == null) {
            throw new BadRequestException("Customer is required");
        }
        if (request.receiptDate() == null) {
            throw new BadRequestException("Receipt date is required");
        }
        if (request.method() == null) {
            throw new BadRequestException("Receipt method is required");
        }
        if (request.allocations() == null || request.allocations().isEmpty()) {
            throw new BadRequestException("A receipt needs at least one invoice allocation");
        }

        Map<UUID, Invoice> invoicesById = new HashMap<>();
        Map<UUID, BigDecimal> allocatedByInvoice = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (AllocationReq allocation : request.allocations()) {
            if (allocation.amount() == null || allocation.amount().signum() <= 0) {
                throw new BadRequestException("Allocation amount must be positive");
            }
            if (allocation.amount().stripTrailingZeros().scale() > 2) {
                throw new BadRequestException("Allocation amount must have at most two decimal places");
            }
            Invoice invoice = invoicesById.computeIfAbsent(allocation.invoiceId(),
                    id -> invoiceRepository.findByIdForUpdate(id)
                            .orElseThrow(() -> new NotFoundException("Invoice not found")));
            if (!request.customerId().equals(invoice.getCustomerId())) {
                throw new BadRequestException(
                        "Invoice " + invoice.getInvoiceNumber() + " belongs to another customer");
            }
            if (invoice.getStatus() != InvoiceStatus.APPROVED && invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID) {
                throw new BadRequestException(
                        "Invoice " + invoice.getInvoiceNumber() + " is not open for receipt");
            }
            BigDecimal allocated = allocatedByInvoice.merge(allocation.invoiceId(), allocation.amount(),
                    BigDecimal::add);
            if (allocated.compareTo(remainingDue(invoice)) > 0) {
                throw new BadRequestException(
                        "Allocation exceeds the remaining amount due on invoice " + invoice.getInvoiceNumber());
            }
            total = total.add(allocation.amount());
        }

        Receipt receipt = new Receipt();
        receipt.setReceiptNumber(nextReceiptNumber(request.receiptDate()));
        receipt.setCustomerId(request.customerId());
        receipt.setReceiptDate(request.receiptDate());
        receipt.setMethod(request.method());
        receipt.setReference(request.reference());
        receipt.setTotal(total);
        receipt.setCreatedBy(CurrentActor.name());
        receipt.setCreatedAt(Instant.now());
        List<ReceiptAllocation> allocations = new ArrayList<>();
        for (AllocationReq allocationReq : request.allocations()) {
            ReceiptAllocation allocation = new ReceiptAllocation();
            allocation.setInvoiceId(allocationReq.invoiceId());
            allocation.setAmount(allocationReq.amount());
            allocations.add(allocation);
        }
        receipt.setAllocations(allocations);
        receipt = receiptRepository.save(receipt);

        JournalEntry posted = ledgerPostingService.post(entry(receipt));
        receipt.setJournalEntryId(posted.getId());
        receipt = receiptRepository.save(receipt);

        allocatedByInvoice.forEach((invoiceId, amount) -> {
            Invoice invoice = invoicesById.get(invoiceId);
            BigDecimal received = (invoice.getAmountReceived() == null ? BigDecimal.ZERO
                    : invoice.getAmountReceived()).add(amount);
            invoice.setAmountReceived(received);
            invoice.setStatus(received.compareTo(invoice.getTotal()) >= 0
                    ? InvoiceStatus.PAID
                    : InvoiceStatus.PARTIALLY_PAID);
            invoiceRepository.save(invoice);
        });

        return receipt;
    }

    public Receipt get(UUID id) {
        return receiptRepository.findById(id).orElseThrow(() -> new NotFoundException("Receipt not found"));
    }

    public List<Receipt> search(UUID customerId) {
        return customerId == null ? receiptRepository.findAll() : receiptRepository.findByCustomerId(customerId);
    }

    private BigDecimal remainingDue(Invoice invoice) {
        BigDecimal received = invoice.getAmountReceived() == null ? BigDecimal.ZERO : invoice.getAmountReceived();
        return invoice.getTotal().subtract(received);
    }

    private JournalEntry entry(Receipt receipt) {
        AccountMappingKey debitKey = receipt.getMethod() == PaymentMethod.CASH
                ? AccountMappingKey.CASH
                : AccountMappingKey.BANK;

        JournalLine debit = new JournalLine();
        debit.setAccountId(mappedAccount(debitKey));
        debit.setDebit(receipt.getTotal());
        JournalLine credit = new JournalLine();
        credit.setAccountId(mappedAccount(AccountMappingKey.ACCOUNTS_RECEIVABLE));
        credit.setCredit(receipt.getTotal());

        JournalEntry draft = new JournalEntry();
        draft.setEntryDate(receipt.getReceiptDate());
        draft.setSourceType(JournalSourceType.AR_RECEIPT);
        draft.setSourceId(receipt.getId());
        draft.setMemo("Receipt " + receipt.getReceiptNumber());
        draft.setCreatedBy(receipt.getCreatedBy());
        draft.setLines(List.of(debit, credit));
        return draft;
    }

    private UUID mappedAccount(AccountMappingKey key) {
        return accountMappingRepository.findAccountId(key)
                .orElseThrow(() -> new BadRequestException("No account mapped for " + key));
    }

    private String nextReceiptNumber(LocalDate receiptDate) {
        String prefix = "RCT-" + NUMBER_MONTH.format(receiptDate) + "-";
        long sequence = receiptRepository.findAll().stream()
                .filter(receipt -> receipt.getReceiptNumber() != null
                        && receipt.getReceiptNumber().startsWith(prefix))
                .count() + 1;
        // Skip numbers already taken (gaps, concurrent creates); the unique column is the backstop.
        String candidate = prefix + String.format("%04d", sequence);
        while (receiptRepository.existsByReceiptNumber(candidate)) {
            sequence++;
            candidate = prefix + String.format("%04d", sequence);
        }
        return candidate;
    }
}
