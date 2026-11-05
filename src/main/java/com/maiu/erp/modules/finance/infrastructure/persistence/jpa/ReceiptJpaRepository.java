package com.maiu.erp.modules.finance.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.finance.infrastructure.persistence.entity.ReceiptEntity;

public interface ReceiptJpaRepository extends JpaRepository<ReceiptEntity, UUID> {
    List<ReceiptEntity> findByCustomerId(UUID customerId);

    boolean existsByReceiptNumber(String receiptNumber);
}
