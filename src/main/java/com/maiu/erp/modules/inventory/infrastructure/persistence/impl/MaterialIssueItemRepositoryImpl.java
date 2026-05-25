package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.inventory.domain.model.MaterialIssueItem;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueItemRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.MaterialIssueItemEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.MaterialIssueItemJpaRepository;

@Repository
public class MaterialIssueItemRepositoryImpl implements MaterialIssueItemRepository {
    private final MaterialIssueItemJpaRepository jpaRepository;

    public MaterialIssueItemRepositoryImpl(MaterialIssueItemJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MaterialIssueItem save(MaterialIssueItem item) {
        return toDomain(jpaRepository.save(toEntity(item)));
    }

    @Override
    public List<MaterialIssueItem> findByMaterialIssueId(UUID materialIssueId) {
        return jpaRepository.findByMaterialIssueId(materialIssueId).stream().map(this::toDomain).toList();
    }

    private MaterialIssueItem toDomain(MaterialIssueItemEntity entity) {
        MaterialIssueItem item = new MaterialIssueItem();
        item.setId(entity.getId());
        item.setMaterialIssueId(entity.getMaterialIssueId());
        item.setProductId(entity.getProductId());
        item.setQuantity(entity.getQuantity());
        item.setUnitCost(entity.getUnitCost());
        item.setLineTotal(entity.getLineTotal());
        return item;
    }

    private MaterialIssueItemEntity toEntity(MaterialIssueItem item) {
        MaterialIssueItemEntity entity = new MaterialIssueItemEntity();
        entity.setId(item.getId());
        entity.setMaterialIssueId(item.getMaterialIssueId());
        entity.setProductId(item.getProductId());
        entity.setQuantity(item.getQuantity());
        entity.setUnitCost(item.getUnitCost());
        entity.setLineTotal(item.getLineTotal());
        return entity;
    }
}
