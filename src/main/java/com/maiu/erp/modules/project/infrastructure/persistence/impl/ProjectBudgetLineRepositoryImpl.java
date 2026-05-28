package com.maiu.erp.modules.project.infrastructure.persistence.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.project.domain.model.ProjectBudgetLine;
import com.maiu.erp.modules.project.domain.repository.ProjectBudgetLineRepository;
import com.maiu.erp.modules.project.infrastructure.persistence.entity.ProjectBudgetLineEntity;
import com.maiu.erp.modules.project.infrastructure.persistence.jpa.ProjectBudgetLineJpaRepository;

@Repository
public class ProjectBudgetLineRepositoryImpl implements ProjectBudgetLineRepository {
    private final ProjectBudgetLineJpaRepository jpaRepository;

    public ProjectBudgetLineRepositoryImpl(ProjectBudgetLineJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ProjectBudgetLine save(ProjectBudgetLine budgetLine) {
        return toDomain(jpaRepository.save(toEntity(budgetLine)));
    }

    @Override
    public Optional<ProjectBudgetLine> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ProjectBudgetLine> findByProjectIdAndCostCode(UUID projectId, String costCode) {
        return jpaRepository.findByProjectIdAndCostCode(projectId, costCode).map(this::toDomain);
    }

    @Override
    public List<ProjectBudgetLine> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectIdOrderByCostCodeAsc(projectId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private ProjectBudgetLine toDomain(ProjectBudgetLineEntity entity) {
        ProjectBudgetLine budgetLine = new ProjectBudgetLine();
        budgetLine.setId(entity.getId());
        budgetLine.setProjectId(entity.getProjectId());
        budgetLine.setCostCode(entity.getCostCode());
        budgetLine.setDescription(entity.getDescription());
        budgetLine.setBudgetAmount(entity.getBudgetAmount());
        budgetLine.setCreatedAt(entity.getCreatedAt());
        budgetLine.setUpdatedAt(entity.getUpdatedAt());
        return budgetLine;
    }

    private ProjectBudgetLineEntity toEntity(ProjectBudgetLine budgetLine) {
        ProjectBudgetLineEntity entity = new ProjectBudgetLineEntity();
        entity.setId(budgetLine.getId());
        entity.setProjectId(budgetLine.getProjectId());
        entity.setCostCode(budgetLine.getCostCode());
        entity.setDescription(budgetLine.getDescription());
        entity.setBudgetAmount(budgetLine.getBudgetAmount());
        entity.setCreatedAt(budgetLine.getCreatedAt());
        entity.setUpdatedAt(budgetLine.getUpdatedAt());
        return entity;
    }
}
