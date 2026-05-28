package com.maiu.erp.modules.project.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.project.domain.model.ProjectBudgetLine;

public interface ProjectBudgetLineRepository {
    ProjectBudgetLine save(ProjectBudgetLine budgetLine);

    Optional<ProjectBudgetLine> findById(UUID id);

    Optional<ProjectBudgetLine> findByProjectIdAndCostCode(UUID projectId, String costCode);

    List<ProjectBudgetLine> findByProjectId(UUID projectId);

    void deleteById(UUID id);
}
