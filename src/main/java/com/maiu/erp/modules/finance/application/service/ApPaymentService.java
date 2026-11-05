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
import com.maiu.erp.modules.finance.domain.enums.BillStatus;
import com.maiu.erp.modules.finance.domain.enums.JournalSourceType;
import com.maiu.erp.modules.finance.domain.enums.PaymentMethod;
import com.maiu.erp.modules.finance.domain.model.Bill;
import com.maiu.erp.modules.finance.domain.model.JournalEntry;
import com.maiu.erp.modules.finance.domain.model.JournalLine;
import com.maiu.erp.modules.finance.domain.model.Payment;
import com.maiu.erp.modules.finance.domain.model.PaymentAllocation;
import com.maiu.erp.modules.finance.domain.repository.AccountMappingRepository;
import com.maiu.erp.modules.finance.domain.repository.BillRepository;
import com.maiu.erp.modules.finance.domain.repository.PaymentRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;

/**
 * Supplier payments. A payment settles one or more approved bills of one supplier;
 * posting (Dr AP / Cr Cash or Bank) happens in the same transaction as the bill updates.
 */
@Service
public class ApPaymentService {
    private static final DateTimeFormatter NUMBER_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final LedgerPostingService ledgerPostingService;
    private final AccountMappingRepository accountMappingRepository;

    public ApPaymentService(
            PaymentRepository paymentRepository,
            BillRepository billRepository,
            LedgerPostingService ledgerPostingService,
            AccountMappingRepository accountMappingRepository) {
        this.paymentRepository = paymentRepository;
        this.billRepository = billRepository;
        this.ledgerPostingService = ledgerPostingService;
        this.accountMappingRepository = accountMappingRepository;
    }

    public record CreatePaymentRequest(
            UUID supplierId,
            LocalDate paymentDate,
            PaymentMethod method,
            String reference,
            List<AllocationReq> allocations) {
    }

    public record AllocationReq(UUID billId, BigDecimal amount) {
    }

    @Transactional
    public Payment create(CreatePaymentRequest request) {
        if (request.supplierId() == null) {
            throw new BadRequestException("Supplier is required");
        }
        if (request.paymentDate() == null) {
            throw new BadRequestException("Payment date is required");
        }
        if (request.method() == null) {
            throw new BadRequestException("Payment method is required");
        }
        if (request.allocations() == null || request.allocations().isEmpty()) {
            throw new BadRequestException("A payment needs at least one bill allocation");
        }

        Map<UUID, Bill> billsById = new HashMap<>();
        Map<UUID, BigDecimal> allocatedByBill = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (AllocationReq allocation : request.allocations()) {
            if (allocation.amount() == null || allocation.amount().signum() <= 0) {
                throw new BadRequestException("Allocation amount must be positive");
            }
            Bill bill = billsById.computeIfAbsent(allocation.billId(), id -> billRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Bill not found")));
            if (!request.supplierId().equals(bill.getSupplierId())) {
                throw new BadRequestException("Bill " + bill.getBillNumber() + " belongs to another supplier");
            }
            if (bill.getStatus() != BillStatus.APPROVED && bill.getStatus() != BillStatus.PARTIALLY_PAID) {
                throw new BadRequestException("Bill " + bill.getBillNumber() + " is not open for payment");
            }
            BigDecimal allocated = allocatedByBill.merge(allocation.billId(), allocation.amount(), BigDecimal::add);
            if (allocated.compareTo(remainingDue(bill)) > 0) {
                throw new BadRequestException(
                        "Allocation exceeds the remaining amount due on bill " + bill.getBillNumber());
            }
            total = total.add(allocation.amount());
        }

        Payment payment = new Payment();
        payment.setPaymentNumber(nextPaymentNumber(request.paymentDate()));
        payment.setSupplierId(request.supplierId());
        payment.setPaymentDate(request.paymentDate());
        payment.setMethod(request.method());
        payment.setReference(request.reference());
        payment.setTotal(total);
        payment.setCreatedBy(CurrentActor.name());
        payment.setCreatedAt(Instant.now());
        List<PaymentAllocation> allocations = new ArrayList<>();
        for (AllocationReq allocationReq : request.allocations()) {
            PaymentAllocation allocation = new PaymentAllocation();
            allocation.setBillId(allocationReq.billId());
            allocation.setAmount(allocationReq.amount());
            allocations.add(allocation);
        }
        payment.setAllocations(allocations);
        payment = paymentRepository.save(payment);

        JournalEntry posted = ledgerPostingService.post(entry(payment));
        payment.setJournalEntryId(posted.getId());
        payment = paymentRepository.save(payment);

        allocatedByBill.forEach((billId, amount) -> {
            Bill bill = billsById.get(billId);
            BigDecimal paid = (bill.getAmountPaid() == null ? BigDecimal.ZERO : bill.getAmountPaid()).add(amount);
            bill.setAmountPaid(paid);
            bill.setStatus(paid.compareTo(bill.getTotal()) >= 0 ? BillStatus.PAID : BillStatus.PARTIALLY_PAID);
            billRepository.save(bill);
        });

        return payment;
    }

    public Payment get(UUID id) {
        return paymentRepository.findById(id).orElseThrow(() -> new NotFoundException("Payment not found"));
    }

    public List<Payment> search(UUID supplierId) {
        return supplierId == null ? paymentRepository.findAll() : paymentRepository.findBySupplierId(supplierId);
    }

    private BigDecimal remainingDue(Bill bill) {
        BigDecimal paid = bill.getAmountPaid() == null ? BigDecimal.ZERO : bill.getAmountPaid();
        return bill.getTotal().subtract(paid);
    }

    private JournalEntry entry(Payment payment) {
        AccountMappingKey creditKey = payment.getMethod() == PaymentMethod.CASH
                ? AccountMappingKey.CASH
                : AccountMappingKey.BANK;

        JournalLine debit = new JournalLine();
        debit.setAccountId(mappedAccount(AccountMappingKey.ACCOUNTS_PAYABLE));
        debit.setDebit(payment.getTotal());
        JournalLine credit = new JournalLine();
        credit.setAccountId(mappedAccount(creditKey));
        credit.setCredit(payment.getTotal());

        JournalEntry draft = new JournalEntry();
        draft.setEntryDate(payment.getPaymentDate());
        draft.setSourceType(JournalSourceType.AP_PAYMENT);
        draft.setSourceId(payment.getId());
        draft.setMemo("Payment " + payment.getPaymentNumber());
        draft.setCreatedBy(payment.getCreatedBy());
        draft.setLines(List.of(debit, credit));
        return draft;
    }

    private UUID mappedAccount(AccountMappingKey key) {
        return accountMappingRepository.findAccountId(key)
                .orElseThrow(() -> new BadRequestException("No account mapped for " + key));
    }

    private String nextPaymentNumber(LocalDate paymentDate) {
        String month = NUMBER_MONTH.format(paymentDate);
        String prefix = "PAY-" + month + "-";
        long inMonth = paymentRepository.findAll().stream()
                .filter(payment -> payment.getPaymentNumber() != null && payment.getPaymentNumber().startsWith(prefix))
                .count();
        return prefix + String.format("%04d", inMonth + 1);
    }
}
