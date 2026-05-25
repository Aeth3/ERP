package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.PurchaseOrderEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.PurchaseOrderJpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PurchaseOrderRepositoryImpl
        implements PurchaseOrderRepository {

    private final PurchaseOrderJpaRepository jpaRepository;

    public PurchaseOrderRepositoryImpl(
            PurchaseOrderJpaRepository jpaRepository) {

        this.jpaRepository = jpaRepository;
    }

    @Override
    public PurchaseOrder save(
            PurchaseOrder purchaseOrder) {

        PurchaseOrderEntity entity =
                toEntity(purchaseOrder);

        PurchaseOrderEntity saved =
                jpaRepository.save(entity);

        return toDomain(saved);
    }

    @Override
    public Optional<PurchaseOrder> findById(UUID id) {

        return jpaRepository
                .findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<PurchaseOrder> findByPoNumber(
            String poNumber) {

        return jpaRepository
                .findByPoNumber(poNumber)
                .map(this::toDomain);
    }

    @Override
    public List<PurchaseOrder> findAll() {

        return jpaRepository
                .findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PurchaseOrder> findBySupplierId(
            UUID supplierId) {

        return jpaRepository
                .findBySupplierId(supplierId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PurchaseOrder> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectId(projectId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private PurchaseOrder toDomain(
            PurchaseOrderEntity entity) {

        PurchaseOrder po = new PurchaseOrder();

        po.setId(entity.getId());

        po.setPoNumber(
                entity.getPoNumber());

        po.setSupplierId(
                entity.getSupplierId());

        po.setProjectId(
                entity.getProjectId());

        po.setStatus(
                entity.getStatus());

        po.setOrderDate(
                entity.getOrderDate());

        po.setExpectedDate(
                entity.getExpectedDate());

        po.setTotalAmount(
                entity.getTotalAmount());

        po.setCreatedAt(
                entity.getCreatedAt());

        return po;
    }

    private PurchaseOrderEntity toEntity(
            PurchaseOrder purchaseOrder) {

        PurchaseOrderEntity entity =
                new PurchaseOrderEntity();

        entity.setId(
                purchaseOrder.getId());

        entity.setPoNumber(
                purchaseOrder.getPoNumber());

        entity.setSupplierId(
                purchaseOrder.getSupplierId());

        entity.setProjectId(
                purchaseOrder.getProjectId());

        entity.setStatus(
                purchaseOrder.getStatus());

        entity.setOrderDate(
                purchaseOrder.getOrderDate());

        entity.setExpectedDate(
                purchaseOrder.getExpectedDate());

        entity.setTotalAmount(
                purchaseOrder.getTotalAmount());

        entity.setCreatedAt(
                purchaseOrder.getCreatedAt());

        return entity;
    }
}
