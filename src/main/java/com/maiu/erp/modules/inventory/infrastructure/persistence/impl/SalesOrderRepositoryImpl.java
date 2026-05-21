package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import com.maiu.erp.modules.inventory.domain.model.SalesOrder;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.SalesOrderEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.SalesOrderJpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SalesOrderRepositoryImpl
        implements SalesOrderRepository {

    private final SalesOrderJpaRepository jpaRepository;

    public SalesOrderRepositoryImpl(
            SalesOrderJpaRepository jpaRepository) {

        this.jpaRepository = jpaRepository;
    }

    @Override
    public SalesOrder save(SalesOrder salesOrder) {

        SalesOrderEntity entity =
                toEntity(salesOrder);

        SalesOrderEntity saved =
                jpaRepository.save(entity);

        return toDomain(saved);
    }

    @Override
    public Optional<SalesOrder> findById(UUID id) {

        return jpaRepository
                .findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<SalesOrder> findBySoNumber(
            String soNumber) {

        return jpaRepository
                .findBySoNumber(soNumber)
                .map(this::toDomain);
    }

    @Override
    public List<SalesOrder> findAll() {

        return jpaRepository
                .findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<SalesOrder> findByCustomerId(
            UUID customerId) {

        return jpaRepository
                .findByCustomerId(customerId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private SalesOrder toDomain(
            SalesOrderEntity entity) {

        SalesOrder so = new SalesOrder();

        so.setId(entity.getId());

        so.setSoNumber(
                entity.getSoNumber());

        so.setCustomerId(
                entity.getCustomerId());

        so.setStatus(
                entity.getStatus());

        so.setOrderDate(
                entity.getOrderDate());

        so.setTotalAmount(
                entity.getTotalAmount());

        return so;
    }

    private SalesOrderEntity toEntity(
            SalesOrder salesOrder) {

        SalesOrderEntity entity =
                new SalesOrderEntity();

        entity.setId(
                salesOrder.getId());

        entity.setSoNumber(
                salesOrder.getSoNumber());

        entity.setCustomerId(
                salesOrder.getCustomerId());

        entity.setStatus(
                salesOrder.getStatus());

        entity.setOrderDate(
                salesOrder.getOrderDate());

        entity.setTotalAmount(
                salesOrder.getTotalAmount());

        return entity;
    }
}