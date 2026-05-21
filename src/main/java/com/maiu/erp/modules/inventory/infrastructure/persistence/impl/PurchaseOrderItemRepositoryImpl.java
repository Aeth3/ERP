package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import com.maiu.erp.modules.inventory.domain.model.PurchaseOrderItem;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderItemRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.PurchaseOrderItemEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.PurchaseOrderItemJpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class PurchaseOrderItemRepositoryImpl
        implements PurchaseOrderItemRepository {

    private final PurchaseOrderItemJpaRepository jpaRepository;

    public PurchaseOrderItemRepositoryImpl(
            PurchaseOrderItemJpaRepository jpaRepository) {

        this.jpaRepository = jpaRepository;
    }

    @Override
    public PurchaseOrderItem save(
            PurchaseOrderItem item) {

        PurchaseOrderItemEntity entity =
                toEntity(item);

        PurchaseOrderItemEntity saved =
                jpaRepository.save(entity);

        return toDomain(saved);
    }

    @Override
    public List<PurchaseOrderItem>
    findByPurchaseOrderId(UUID purchaseOrderId) {

        return jpaRepository
                .findByPurchaseOrderId(purchaseOrderId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteByPurchaseOrderId(
            UUID purchaseOrderId) {

        jpaRepository.deleteByPurchaseOrderId(
                purchaseOrderId);
    }

    private PurchaseOrderItem toDomain(
            PurchaseOrderItemEntity entity) {

        PurchaseOrderItem item =
                new PurchaseOrderItem();

        item.setId(entity.getId());

        item.setPurchaseOrderId(
                entity.getPurchaseOrderId());

        item.setProductId(
                entity.getProductId());

        item.setQuantity(
                entity.getQuantity());

        item.setUnitCost(
                entity.getUnitCost());

        return item;
    }

    private PurchaseOrderItemEntity toEntity(
            PurchaseOrderItem item) {

        PurchaseOrderItemEntity entity =
                new PurchaseOrderItemEntity();

        entity.setId(item.getId());

        entity.setPurchaseOrderId(
                item.getPurchaseOrderId());

        entity.setProductId(
                item.getProductId());

        entity.setQuantity(
                item.getQuantity());

        entity.setUnitCost(
                item.getUnitCost());

        return entity;
    }
}