package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.PurchaseReturnEntity;

public interface PurchaseReturnJpaRepository extends JpaRepository<PurchaseReturnEntity, UUID> {
    Optional<PurchaseReturnEntity> findByReturnNumber(String returnNumber);

    List<PurchaseReturnEntity> findByPurchaseOrderId(UUID purchaseOrderId);
}
