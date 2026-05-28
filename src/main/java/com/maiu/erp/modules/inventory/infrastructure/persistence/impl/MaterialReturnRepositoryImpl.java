package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.inventory.domain.model.MaterialReturn;
import com.maiu.erp.modules.inventory.domain.repository.MaterialReturnRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.MaterialReturnEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.MaterialReturnJpaRepository;

@Repository
public class MaterialReturnRepositoryImpl implements MaterialReturnRepository {
    private final MaterialReturnJpaRepository jpaRepository;

    public MaterialReturnRepositoryImpl(MaterialReturnJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MaterialReturn save(MaterialReturn materialReturn) {
        return toDomain(jpaRepository.save(toEntity(materialReturn)));
    }

    @Override
    public Optional<MaterialReturn> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<MaterialReturn> findByReturnNumber(String returnNumber) {
        return jpaRepository.findByReturnNumber(returnNumber).map(this::toDomain);
    }

    @Override
    public List<MaterialReturn> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<MaterialReturn> findByMaterialIssueId(UUID materialIssueId) {
        return jpaRepository.findByMaterialIssueId(materialIssueId).stream().map(this::toDomain).toList();
    }

    private MaterialReturn toDomain(MaterialReturnEntity entity) {
        MaterialReturn materialReturn = new MaterialReturn();
        materialReturn.setId(entity.getId());
        materialReturn.setReturnNumber(entity.getReturnNumber());
        materialReturn.setMaterialIssueId(entity.getMaterialIssueId());
        materialReturn.setProjectId(entity.getProjectId());
        materialReturn.setWarehouseId(entity.getWarehouseId());
        materialReturn.setReversal(entity.isReversal());
        materialReturn.setRemarks(entity.getRemarks());
        materialReturn.setPerformedBy(entity.getPerformedBy());
        materialReturn.setReturnedAt(entity.getReturnedAt());
        return materialReturn;
    }

    private MaterialReturnEntity toEntity(MaterialReturn materialReturn) {
        MaterialReturnEntity entity = new MaterialReturnEntity();
        entity.setId(materialReturn.getId());
        entity.setReturnNumber(materialReturn.getReturnNumber());
        entity.setMaterialIssueId(materialReturn.getMaterialIssueId());
        entity.setProjectId(materialReturn.getProjectId());
        entity.setWarehouseId(materialReturn.getWarehouseId());
        entity.setReversal(materialReturn.isReversal());
        entity.setRemarks(materialReturn.getRemarks());
        entity.setPerformedBy(materialReturn.getPerformedBy());
        entity.setReturnedAt(materialReturn.getReturnedAt());
        return entity;
    }
}
