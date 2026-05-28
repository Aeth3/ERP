package com.maiu.erp.modules.project.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.project.infrastructure.persistence.entity.ProjectBudgetLineEntity;

public interface ProjectBudgetLineJpaRepository extends JpaRepository<ProjectBudgetLineEntity, UUID> {
    Optional<ProjectBudgetLineEntity> findByProjectIdAndCostCode(UUID projectId, String costCode);

    List<ProjectBudgetLineEntity> findByProjectIdOrderByCostCodeAsc(UUID projectId);
}
