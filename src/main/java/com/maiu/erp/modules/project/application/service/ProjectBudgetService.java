package com.maiu.erp.modules.project.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.project.application.dto.CreateProjectBudgetLineRequest;
import com.maiu.erp.modules.project.domain.model.ProjectBudgetLine;
import com.maiu.erp.modules.project.domain.repository.ProjectBudgetLineRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.ConflictException;
import com.maiu.erp.shared.exception.NotFoundException;

@Service
public class ProjectBudgetService {
    private final ProjectService projectService;
    private final ProjectBudgetLineRepository projectBudgetLineRepository;

    public ProjectBudgetService(
            ProjectService projectService,
            ProjectBudgetLineRepository projectBudgetLineRepository) {
        this.projectService = projectService;
        this.projectBudgetLineRepository = projectBudgetLineRepository;
    }

    public UUID createBudgetLine(UUID projectId, CreateProjectBudgetLineRequest request) {
        projectService.getProjectById(projectId);

        String costCode = normalizeRequired(request.costCode(), "Cost code is required");
        if (projectBudgetLineRepository.findByProjectIdAndCostCode(projectId, costCode).isPresent()) {
            throw new ConflictException("Project budget cost code already exists");
        }

        ProjectBudgetLine budgetLine = new ProjectBudgetLine();
        budgetLine.setProjectId(projectId);
        budgetLine.setCostCode(costCode);
        budgetLine.setDescription(normalizeRequired(request.description(), "Description is required"));
        budgetLine.setBudgetAmount(normalizeBudgetAmount(request.budgetAmount()));
        budgetLine.setCreatedAt(Instant.now());
        budgetLine.setUpdatedAt(Instant.now());
        return projectBudgetLineRepository.save(budgetLine).getId();
    }

    public List<ProjectBudgetLine> getBudgetLines(UUID projectId) {
        projectService.getProjectById(projectId);
        return projectBudgetLineRepository.findByProjectId(projectId);
    }

    public void updateBudgetLine(UUID projectId, UUID budgetLineId, CreateProjectBudgetLineRequest request) {
        projectService.getProjectById(projectId);
        ProjectBudgetLine budgetLine = getOwnedBudgetLine(projectId, budgetLineId);
        String costCode = normalizeRequired(request.costCode(), "Cost code is required");

        projectBudgetLineRepository.findByProjectIdAndCostCode(projectId, costCode)
                .filter(existing -> !existing.getId().equals(budgetLineId))
                .ifPresent(existing -> {
                    throw new ConflictException("Project budget cost code already exists");
                });

        budgetLine.setCostCode(costCode);
        budgetLine.setDescription(normalizeRequired(request.description(), "Description is required"));
        budgetLine.setBudgetAmount(normalizeBudgetAmount(request.budgetAmount()));
        budgetLine.setUpdatedAt(Instant.now());
        projectBudgetLineRepository.save(budgetLine);
    }

    public void deleteBudgetLine(UUID projectId, UUID budgetLineId) {
        projectService.getProjectById(projectId);
        getOwnedBudgetLine(projectId, budgetLineId);
        projectBudgetLineRepository.deleteById(budgetLineId);
    }

    private ProjectBudgetLine getOwnedBudgetLine(UUID projectId, UUID budgetLineId) {
        ProjectBudgetLine budgetLine = projectBudgetLineRepository.findById(budgetLineId)
                .orElseThrow(() -> new NotFoundException("Project budget line not found"));
        if (!projectId.equals(budgetLine.getProjectId())) {
            throw new NotFoundException("Project budget line not found");
        }
        return budgetLine;
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }

        return value.trim();
    }

    private BigDecimal normalizeBudgetAmount(BigDecimal budgetAmount) {
        if (budgetAmount == null) {
            throw new BadRequestException("Budget amount is required");
        }

        if (budgetAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Budget amount must be zero or greater");
        }

        return budgetAmount;
    }
}
