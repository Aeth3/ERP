package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.inventory.domain.model.PurchaseReturnItem;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseReturnItemRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.PurchaseReturnItemEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.PurchaseReturnItemJpaRepository;

@Repository
public class PurchaseReturnItemRepositoryImpl implements PurchaseReturnItemRepository {
    private final PurchaseReturnItemJpaRepository jpaRepository;

    public PurchaseReturnItemRepositoryImpl(PurchaseReturnItemJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PurchaseReturnItem save(PurchaseReturnItem item) {
        return toDomain(jpaRepository.save(toEntity(item)));
    }

    @Override
    public List<PurchaseReturnItem> findByPurchaseReturnId(UUID purchaseReturnId) {
        return jpaRepository.findByPurchaseReturnId(purchaseReturnId).stream().map(this::toDomain).toList();
    }

    private PurchaseReturnItem toDomain(PurchaseReturnItemEntity entity) {
        PurchaseReturnItem item = new PurchaseReturnItem();
        item.setId(entity.getId());
        item.setPurchaseReturnId(entity.getPurchaseReturnId());
        item.setProductId(entity.getProductId());
        item.setQuantity(entity.getQuantity());
        item.setUnitCost(entity.getUnitCost());
        item.setLineTotal(entity.getLineTotal());
        return item;
    }

    private PurchaseReturnItemEntity toEntity(PurchaseReturnItem item) {
        PurchaseReturnItemEntity entity = new PurchaseReturnItemEntity();
        entity.setId(item.getId());
        entity.setPurchaseReturnId(item.getPurchaseReturnId());
        entity.setProductId(item.getProductId());
        entity.setQuantity(item.getQuantity());
        entity.setUnitCost(item.getUnitCost());
        entity.setLineTotal(item.getLineTotal());
        return entity;
    }
}
