package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.PurchaseOrderItemEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurchaseOrderItemJpaRepository
        extends JpaRepository<PurchaseOrderItemEntity, UUID> {

    List<PurchaseOrderItemEntity>
    findByPurchaseOrderId(UUID purchaseOrderId);

    void deleteByPurchaseOrderId(
            UUID purchaseOrderId);
}