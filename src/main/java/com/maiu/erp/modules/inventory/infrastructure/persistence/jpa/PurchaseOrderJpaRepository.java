package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.PurchaseOrderEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderJpaRepository
        extends JpaRepository<PurchaseOrderEntity, UUID> {

    Optional<PurchaseOrderEntity>
    findByPoNumber(String poNumber);

    List<PurchaseOrderEntity>
    findBySupplierId(UUID supplierId);
}