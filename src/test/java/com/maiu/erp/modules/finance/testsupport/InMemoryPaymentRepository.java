package com.maiu.erp.modules.finance.testsupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.model.Payment;
import com.maiu.erp.modules.finance.domain.repository.PaymentRepository;

public final class InMemoryPaymentRepository implements PaymentRepository {
    private final List<Payment> payments = new ArrayList<>();

    @Override
    public Payment save(Payment payment) {
        if (payment.getId() == null) {
            payment.setId(UUID.randomUUID());
            payments.add(payment);
        } else if (payments.stream().noneMatch(existing -> payment.getId().equals(existing.getId()))) {
            payments.add(payment);
        } else {
            payments.replaceAll(existing -> payment.getId().equals(existing.getId()) ? payment : existing);
        }
        return payment;
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return payments.stream().filter(payment -> id.equals(payment.getId())).findFirst();
    }

    @Override
    public List<Payment> findBySupplierId(UUID supplierId) {
        return payments.stream().filter(payment -> supplierId.equals(payment.getSupplierId())).toList();
    }

    @Override
    public List<Payment> findAll() {
        return List.copyOf(payments);
    }
}
