package com.maiu.erp.modules.project.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.model.MaterialIssue;
import com.maiu.erp.modules.inventory.domain.model.MaterialReturn;
import com.maiu.erp.modules.inventory.domain.model.StockMovement;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialReturnItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialReturnRepository;
import com.maiu.erp.modules.inventory.domain.repository.StockMovementRepository;
import com.maiu.erp.modules.project.application.dto.ProjectCostSummaryDto;
import com.maiu.erp.modules.project.application.dto.ProjectDashboardDto;
import com.maiu.erp.modules.project.domain.model.Project;

@Service
public class ProjectDashboardQueryService {
    private final ProjectService projectService;
    private final ProjectCostQueryService projectCostQueryService;
    private final MaterialIssueRepository materialIssueRepository;
    private final MaterialIssueItemRepository materialIssueItemRepository;
    private final MaterialReturnRepository materialReturnRepository;
    private final MaterialReturnItemRepository materialReturnItemRepository;
    private final StockMovementRepository stockMovementRepository;

    public ProjectDashboardQueryService(
            ProjectService projectService,
            ProjectCostQueryService projectCostQueryService,
            MaterialIssueRepository materialIssueRepository,
            MaterialIssueItemRepository materialIssueItemRepository,
            MaterialReturnRepository materialReturnRepository,
            MaterialReturnItemRepository materialReturnItemRepository,
            StockMovementRepository stockMovementRepository) {
        this.projectService = projectService;
        this.projectCostQueryService = projectCostQueryService;
        this.materialIssueRepository = materialIssueRepository;
        this.materialIssueItemRepository = materialIssueItemRepository;
        this.materialReturnRepository = materialReturnRepository;
        this.materialReturnItemRepository = materialReturnItemRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public ProjectDashboardDto getProjectDashboard(UUID projectId) {
        Project project = projectService.getProjectById(projectId);
        ProjectCostSummaryDto costSummary = projectCostQueryService.getProjectCostSummary(projectId);
        List<MaterialIssue> issues = materialIssueRepository.findByProjectId(projectId);
        List<MaterialReturn> returns = issues.stream()
                .map(issue -> materialReturnRepository.findByMaterialIssueId(issue.getId()))
                .flatMap(List::stream)
                .toList();
        List<StockMovement> movements = stockMovementRepository.findByProjectId(projectId);

        BigDecimal totalIssuedQuantity = issues.stream()
                .map(issue -> materialIssueItemRepository.findByMaterialIssueId(issue.getId()).stream()
                        .map(item -> item.getQuantity())
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReturnedQuantity = returns.stream()
                .map(materialReturn -> materialReturnItemRepository.findByMaterialReturnId(materialReturn.getId()).stream()
                        .map(item -> item.getQuantity())
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netIssuedQuantity = totalIssuedQuantity.subtract(totalReturnedQuantity);
        int totalReversalCount = (int) returns.stream().filter(MaterialReturn::isReversal).count();
        Instant lastMovementDate = movements.stream()
                .map(StockMovement::getMovementDate)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new ProjectDashboardDto(
                project.getId(),
                project.getProjectName(),
                project.getStatus(),
                costSummary.getBudgetBasisAmount(),
                costSummary.getCommittedCost(),
                costSummary.getActualCost(),
                costSummary.getCommittedVariance(),
                costSummary.getActualVariance(),
                calculateBudgetUtilizationPercent(costSummary.getBudgetBasisAmount(), costSummary.getActualCost()),
                totalIssuedQuantity,
                totalReturnedQuantity,
                netIssuedQuantity,
                issues.size(),
                returns.size(),
                totalReversalCount,
                movements.size(),
                lastMovementDate);
    }

    private BigDecimal calculateBudgetUtilizationPercent(BigDecimal budgetBasisAmount, BigDecimal actualCost) {
        if (budgetBasisAmount == null || budgetBasisAmount.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return actualCost.multiply(new BigDecimal("100"))
                .divide(budgetBasisAmount, 2, RoundingMode.HALF_UP);
    }
}
