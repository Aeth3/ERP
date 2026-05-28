package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.inventory.domain.model.SalesReturn;
import com.maiu.erp.modules.inventory.domain.repository.SalesReturnRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.SalesReturnEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.SalesReturnJpaRepository;

@Repository
public class SalesReturnRepositoryImpl implements SalesReturnRepository {
    private final SalesReturnJpaRepository jpaRepository;

    public SalesReturnRepositoryImpl(SalesReturnJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SalesReturn save(SalesReturn salesReturn) {
        return toDomain(jpaRepository.save(toEntity(salesReturn)));
    }

    @Override
    public Optional<SalesReturn> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<SalesReturn> findByReturnNumber(String returnNumber) {
        return jpaRepository.findByReturnNumber(returnNumber).map(this::toDomain);
    }

    @Override
    public List<SalesReturn> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<SalesReturn> findBySalesOrderId(UUID salesOrderId) {
        return jpaRepository.findBySalesOrderId(salesOrderId).stream().map(this::toDomain).toList();
    }

    private SalesReturn toDomain(SalesReturnEntity entity) {
        SalesReturn salesReturn = new SalesReturn();
        salesReturn.setId(entity.getId());
        salesReturn.setReturnNumber(entity.getReturnNumber());
        salesReturn.setSalesOrderId(entity.getSalesOrderId());
        salesReturn.setCustomerId(entity.getCustomerId());
        salesReturn.setWarehouseId(entity.getWarehouseId());
        salesReturn.setRemarks(entity.getRemarks());
        salesReturn.setPerformedBy(entity.getPerformedBy());
        salesReturn.setReturnedAt(entity.getReturnedAt());
        return salesReturn;
    }

    private SalesReturnEntity toEntity(SalesReturn salesReturn) {
        SalesReturnEntity entity = new SalesReturnEntity();
        entity.setId(salesReturn.getId());
        entity.setReturnNumber(salesReturn.getReturnNumber());
        entity.setSalesOrderId(salesReturn.getSalesOrderId());
        entity.setCustomerId(salesReturn.getCustomerId());
        entity.setWarehouseId(salesReturn.getWarehouseId());
        entity.setRemarks(salesReturn.getRemarks());
        entity.setPerformedBy(salesReturn.getPerformedBy());
        entity.setReturnedAt(salesReturn.getReturnedAt());
        return entity;
    }
}
