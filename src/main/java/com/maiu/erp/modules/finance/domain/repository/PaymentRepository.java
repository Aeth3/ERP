package com.maiu.erp.modules.finance.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.model.Payment;

public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    List<Payment> findBySupplierId(UUID supplierId);

    List<Payment> findAll();

    boolean existsByPaymentNumber(String paymentNumber);
}
