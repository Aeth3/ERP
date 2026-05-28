package com.maiu.erp.modules.project.application.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.MaterialReturn;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialReturnItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialReturnRepository;
import com.maiu.erp.modules.project.application.dto.ProjectCostItemDto;
import com.maiu.erp.modules.project.application.dto.ProjectCostSummaryDto;
import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.modules.project.domain.repository.ProjectBudgetLineRepository;

@Service
public class ProjectCostQueryService {
    private final ProjectService projectService;
    private final ProjectBudgetLineRepository projectBudgetLineRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MaterialIssueRepository materialIssueRepository;
    private final MaterialIssueItemRepository materialIssueItemRepository;
    private final MaterialReturnRepository materialReturnRepository;
    private final MaterialReturnItemRepository materialReturnItemRepository;
    private final ProductRepository productRepository;

    public ProjectCostQueryService(
            ProjectService projectService,
            ProjectBudgetLineRepository projectBudgetLineRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            MaterialIssueRepository materialIssueRepository,
            MaterialIssueItemRepository materialIssueItemRepository,
            MaterialReturnRepository materialReturnRepository,
            MaterialReturnItemRepository materialReturnItemRepository,
            ProductRepository productRepository) {
        this.projectService = projectService;
        this.projectBudgetLineRepository = projectBudgetLineRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.materialIssueRepository = materialIssueRepository;
        this.materialIssueItemRepository = materialIssueItemRepository;
        this.materialReturnRepository = materialReturnRepository;
        this.materialReturnItemRepository = materialReturnItemRepository;
        this.productRepository = productRepository;
    }

    public ProjectCostSummaryDto getProjectCostSummary(UUID projectId) {
        Project project = projectService.getProjectById(projectId);
        BigDecimal totalBudgetLineAmount = projectBudgetLineRepository.findByProjectId(projectId)
                .stream()
                .map(budgetLine -> budgetLine.getBudgetAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPurchaseOrderAmount = purchaseOrderRepository.findByProjectId(projectId)
                .stream()
                .map(PurchaseOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<UUID, ProjectCostAccumulator> byProduct = new LinkedHashMap<>();
        materialIssueRepository.findByProjectId(projectId)
                .forEach(issue -> materialIssueItemRepository.findByMaterialIssueId(issue.getId())
                        .forEach(item -> byProduct.computeIfAbsent(item.getProductId(), ignored -> new ProjectCostAccumulator())
                                .addIssued(item.getQuantity(), item.getLineTotal())));

        materialIssueRepository.findByProjectId(projectId)
                .stream()
                .map(issue -> materialReturnRepository.findByMaterialIssueId(issue.getId()))
                .flatMap(List::stream)
                .forEach(materialReturn -> applyReturnItems(byProduct, materialReturn));

        List<ProjectCostItemDto> items = byProduct.entrySet()
                .stream()
                .map(entry -> new ProjectCostItemDto(
                        entry.getKey(),
                        productRepository.findById(entry.getKey()).map(Product::getName).orElse("Unknown Product"),
                        entry.getValue().issuedQuantity,
                        entry.getValue().issuedCost,
                        entry.getValue().returnedQuantity,
                        entry.getValue().returnedCost,
                        entry.getValue().issuedQuantity.subtract(entry.getValue().returnedQuantity),
                        entry.getValue().issuedCost.subtract(entry.getValue().returnedCost)))
                .toList();

        BigDecimal totalMaterialIssuedCost = items.stream()
                .map(ProjectCostItemDto::getIssuedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMaterialReturnedCost = items.stream()
                .map(ProjectCostItemDto::getReturnedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netMaterialIssuedCost = totalMaterialIssuedCost.subtract(totalMaterialReturnedCost);

        BigDecimal budgetAmount = project.getBudgetAmount();
        BigDecimal budgetBasisAmount = totalBudgetLineAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalBudgetLineAmount
                : budgetAmount;
        BigDecimal committedCost = totalPurchaseOrderAmount;
        BigDecimal actualCost = netMaterialIssuedCost;
        BigDecimal committedVariance = budgetBasisAmount == null
                ? null
                : budgetBasisAmount.subtract(committedCost);
        BigDecimal actualVariance = budgetBasisAmount == null
                ? null
                : budgetBasisAmount.subtract(actualCost);
        BigDecimal budgetVariance = actualVariance;

        return new ProjectCostSummaryDto(
                project.getId(),
                project.getProjectName(),
                budgetAmount,
                totalBudgetLineAmount,
                budgetBasisAmount,
                totalPurchaseOrderAmount,
                totalMaterialIssuedCost,
                totalMaterialReturnedCost,
                netMaterialIssuedCost,
                committedCost,
                actualCost,
                committedVariance,
                actualVariance,
                budgetVariance,
                items);
    }

    private void applyReturnItems(Map<UUID, ProjectCostAccumulator> byProduct, MaterialReturn materialReturn) {
        materialReturnItemRepository.findByMaterialReturnId(materialReturn.getId())
                .forEach(item -> byProduct.computeIfAbsent(item.getProductId(), ignored -> new ProjectCostAccumulator())
                        .addReturned(item.getQuantity(), item.getLineTotal()));
    }

    private static final class ProjectCostAccumulator {
        private BigDecimal issuedQuantity = BigDecimal.ZERO;
        private BigDecimal issuedCost = BigDecimal.ZERO;
        private BigDecimal returnedQuantity = BigDecimal.ZERO;
        private BigDecimal returnedCost = BigDecimal.ZERO;

        private void addIssued(BigDecimal additionalQuantity, BigDecimal additionalCost) {
            issuedQuantity = issuedQuantity.add(additionalQuantity);
            issuedCost = issuedCost.add(additionalCost);
        }

        private void addReturned(BigDecimal additionalQuantity, BigDecimal additionalCost) {
            returnedQuantity = returnedQuantity.add(additionalQuantity);
            returnedCost = returnedCost.add(additionalCost);
        }
    }
}
