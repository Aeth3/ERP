package com.maiu.erp.modules.finance.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.model.Receipt;

public interface ReceiptRepository {
    Receipt save(Receipt receipt);

    Optional<Receipt> findById(UUID id);

    List<Receipt> findByCustomerId(UUID customerId);

    List<Receipt> findAll();

    boolean existsByReceiptNumber(String receiptNumber);
}
