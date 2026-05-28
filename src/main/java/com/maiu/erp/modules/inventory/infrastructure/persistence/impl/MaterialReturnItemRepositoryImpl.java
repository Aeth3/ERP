package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.inventory.domain.model.MaterialReturnItem;
import com.maiu.erp.modules.inventory.domain.repository.MaterialReturnItemRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.MaterialReturnItemEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.MaterialReturnItemJpaRepository;

@Repository
public class MaterialReturnItemRepositoryImpl implements MaterialReturnItemRepository {
    private final MaterialReturnItemJpaRepository jpaRepository;

    public MaterialReturnItemRepositoryImpl(MaterialReturnItemJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MaterialReturnItem save(MaterialReturnItem item) {
        return toDomain(jpaRepository.save(toEntity(item)));
    }

    @Override
    public List<MaterialReturnItem> findByMaterialReturnId(UUID materialReturnId) {
        return jpaRepository.findByMaterialReturnId(materialReturnId).stream().map(this::toDomain).toList();
    }

    private MaterialReturnItem toDomain(MaterialReturnItemEntity entity) {
        MaterialReturnItem item = new MaterialReturnItem();
        item.setId(entity.getId());
        item.setMaterialReturnId(entity.getMaterialReturnId());
        item.setProductId(entity.getProductId());
        item.setQuantity(entity.getQuantity());
        item.setUnitCost(entity.getUnitCost());
        item.setLineTotal(entity.getLineTotal());
        return item;
    }

    private MaterialReturnItemEntity toEntity(MaterialReturnItem item) {
        MaterialReturnItemEntity entity = new MaterialReturnItemEntity();
        entity.setId(item.getId());
        entity.setMaterialReturnId(item.getMaterialReturnId());
        entity.setProductId(item.getProductId());
        entity.setQuantity(item.getQuantity());
        entity.setUnitCost(item.getUnitCost());
        entity.setLineTotal(item.getLineTotal());
        return entity;
    }
}
