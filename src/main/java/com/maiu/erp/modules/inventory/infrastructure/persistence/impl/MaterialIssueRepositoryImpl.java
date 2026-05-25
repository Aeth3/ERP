package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.inventory.domain.model.MaterialIssue;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.MaterialIssueEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.MaterialIssueJpaRepository;

@Repository
public class MaterialIssueRepositoryImpl implements MaterialIssueRepository {
    private final MaterialIssueJpaRepository jpaRepository;

    public MaterialIssueRepositoryImpl(MaterialIssueJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MaterialIssue save(MaterialIssue materialIssue) {
        return toDomain(jpaRepository.save(toEntity(materialIssue)));
    }

    @Override
    public Optional<MaterialIssue> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<MaterialIssue> findByIssueNumber(String issueNumber) {
        return jpaRepository.findByIssueNumber(issueNumber).map(this::toDomain);
    }

    @Override
    public List<MaterialIssue> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<MaterialIssue> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectId(projectId).stream().map(this::toDomain).toList();
    }

    private MaterialIssue toDomain(MaterialIssueEntity entity) {
        MaterialIssue materialIssue = new MaterialIssue();
        materialIssue.setId(entity.getId());
        materialIssue.setIssueNumber(entity.getIssueNumber());
        materialIssue.setProjectId(entity.getProjectId());
        materialIssue.setWarehouseId(entity.getWarehouseId());
        materialIssue.setRemarks(entity.getRemarks());
        materialIssue.setPerformedBy(entity.getPerformedBy());
        materialIssue.setIssuedAt(entity.getIssuedAt());
        return materialIssue;
    }

    private MaterialIssueEntity toEntity(MaterialIssue materialIssue) {
        MaterialIssueEntity entity = new MaterialIssueEntity();
        entity.setId(materialIssue.getId());
        entity.setIssueNumber(materialIssue.getIssueNumber());
        entity.setProjectId(materialIssue.getProjectId());
        entity.setWarehouseId(materialIssue.getWarehouseId());
        entity.setRemarks(materialIssue.getRemarks());
        entity.setPerformedBy(materialIssue.getPerformedBy());
        entity.setIssuedAt(materialIssue.getIssuedAt());
        return entity;
    }
}
