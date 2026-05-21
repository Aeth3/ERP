package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import com.maiu.erp.modules.inventory.domain.model.SalesOrderItem;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderItemRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.SalesOrderItemEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.SalesOrderItemJpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class SalesOrderItemRepositoryImpl
        implements SalesOrderItemRepository {

    private final SalesOrderItemJpaRepository jpaRepository;

    public SalesOrderItemRepositoryImpl(
            SalesOrderItemJpaRepository jpaRepository) {

        this.jpaRepository = jpaRepository;
    }

    @Override
    public SalesOrderItem save(
            SalesOrderItem item) {

        SalesOrderItemEntity entity =
                toEntity(item);

        SalesOrderItemEntity saved =
                jpaRepository.save(entity);

        return toDomain(saved);
    }

    @Override
    public List<SalesOrderItem>
    findBySalesOrderId(UUID salesOrderId) {

        return jpaRepository
                .findBySalesOrderId(salesOrderId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteBySalesOrderId(
            UUID salesOrderId) {

        jpaRepository.deleteBySalesOrderId(
                salesOrderId);
    }
    private SalesOrderItem toDomain(
            SalesOrderItemEntity entity) {

        SalesOrderItem item =
                new SalesOrderItem();

        item.setId(entity.getId());

        item.setSalesOrderId(
                entity.getSalesOrderId());

        item.setProductId(
                entity.getProductId());

        item.setQuantity(
                entity.getQuantity());

        item.setUnitPrice(
                entity.getUnitPrice());

        return item;
    }

    private SalesOrderItemEntity toEntity(
            SalesOrderItem item) {

        SalesOrderItemEntity entity =
                new SalesOrderItemEntity();

        entity.setId(item.getId());

        entity.setSalesOrderId(
                item.getSalesOrderId());

        entity.setProductId(
                item.getProductId());

        entity.setQuantity(
                item.getQuantity());

        entity.setUnitPrice(
                item.getUnitPrice());

        return entity;
    }
}