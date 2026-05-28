package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.inventory.domain.model.SalesReturnItem;
import com.maiu.erp.modules.inventory.domain.repository.SalesReturnItemRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.SalesReturnItemEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.SalesReturnItemJpaRepository;

@Repository
public class SalesReturnItemRepositoryImpl implements SalesReturnItemRepository {
    private final SalesReturnItemJpaRepository jpaRepository;

    public SalesReturnItemRepositoryImpl(SalesReturnItemJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SalesReturnItem save(SalesReturnItem item) {
        return toDomain(jpaRepository.save(toEntity(item)));
    }

    @Override
    public List<SalesReturnItem> findBySalesReturnId(UUID salesReturnId) {
        return jpaRepository.findBySalesReturnId(salesReturnId).stream().map(this::toDomain).toList();
    }

    private SalesReturnItem toDomain(SalesReturnItemEntity entity) {
        SalesReturnItem item = new SalesReturnItem();
        item.setId(entity.getId());
        item.setSalesReturnId(entity.getSalesReturnId());
        item.setProductId(entity.getProductId());
        item.setQuantity(entity.getQuantity());
        item.setUnitPrice(entity.getUnitPrice());
        item.setLineTotal(entity.getLineTotal());
        return item;
    }

    private SalesReturnItemEntity toEntity(SalesReturnItem item) {
        SalesReturnItemEntity entity = new SalesReturnItemEntity();
        entity.setId(item.getId());
        entity.setSalesReturnId(item.getSalesReturnId());
        entity.setProductId(item.getProductId());
        entity.setQuantity(item.getQuantity());
        entity.setUnitPrice(item.getUnitPrice());
        entity.setLineTotal(item.getLineTotal());
        return entity;
    }
}
