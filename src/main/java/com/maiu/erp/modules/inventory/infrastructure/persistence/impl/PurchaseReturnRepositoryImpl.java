package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.inventory.domain.model.PurchaseReturn;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseReturnRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.PurchaseReturnEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.PurchaseReturnJpaRepository;

@Repository
public class PurchaseReturnRepositoryImpl implements PurchaseReturnRepository {
    private final PurchaseReturnJpaRepository jpaRepository;

    public PurchaseReturnRepositoryImpl(PurchaseReturnJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PurchaseReturn save(PurchaseReturn purchaseReturn) {
        return toDomain(jpaRepository.save(toEntity(purchaseReturn)));
    }

    @Override
    public Optional<PurchaseReturn> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<PurchaseReturn> findByReturnNumber(String returnNumber) {
        return jpaRepository.findByReturnNumber(returnNumber).map(this::toDomain);
    }

    @Override
    public List<PurchaseReturn> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<PurchaseReturn> findByPurchaseOrderId(UUID purchaseOrderId) {
        return jpaRepository.findByPurchaseOrderId(purchaseOrderId).stream().map(this::toDomain).toList();
    }

    private PurchaseReturn toDomain(PurchaseReturnEntity entity) {
        PurchaseReturn purchaseReturn = new PurchaseReturn();
        purchaseReturn.setId(entity.getId());
        purchaseReturn.setReturnNumber(entity.getReturnNumber());
        purchaseReturn.setPurchaseOrderId(entity.getPurchaseOrderId());
        purchaseReturn.setSupplierId(entity.getSupplierId());
        purchaseReturn.setWarehouseId(entity.getWarehouseId());
        purchaseReturn.setRemarks(entity.getRemarks());
        purchaseReturn.setPerformedBy(entity.getPerformedBy());
        purchaseReturn.setReturnedAt(entity.getReturnedAt());
        return purchaseReturn;
    }

    private PurchaseReturnEntity toEntity(PurchaseReturn purchaseReturn) {
        PurchaseReturnEntity entity = new PurchaseReturnEntity();
        entity.setId(purchaseReturn.getId());
        entity.setReturnNumber(purchaseReturn.getReturnNumber());
        entity.setPurchaseOrderId(purchaseReturn.getPurchaseOrderId());
        entity.setSupplierId(purchaseReturn.getSupplierId());
        entity.setWarehouseId(purchaseReturn.getWarehouseId());
        entity.setRemarks(purchaseReturn.getRemarks());
        entity.setPerformedBy(purchaseReturn.getPerformedBy());
        entity.setReturnedAt(purchaseReturn.getReturnedAt());
        return entity;
    }
}
