package com.maiu.erp.modules.project.application.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueRepository;
import com.maiu.erp.modules.project.application.dto.ProjectCostItemDto;
import com.maiu.erp.modules.project.application.dto.ProjectCostSummaryDto;
import com.maiu.erp.modules.project.domain.model.Project;

@Service
public class ProjectCostQueryService {
    private final ProjectService projectService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MaterialIssueRepository materialIssueRepository;
    private final MaterialIssueItemRepository materialIssueItemRepository;
    private final ProductRepository productRepository;

    public ProjectCostQueryService(
            ProjectService projectService,
            PurchaseOrderRepository purchaseOrderRepository,
            MaterialIssueRepository materialIssueRepository,
            MaterialIssueItemRepository materialIssueItemRepository,
            ProductRepository productRepository) {
        this.projectService = projectService;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.materialIssueRepository = materialIssueRepository;
        this.materialIssueItemRepository = materialIssueItemRepository;
        this.productRepository = productRepository;
    }

    public ProjectCostSummaryDto getProjectCostSummary(UUID projectId) {
        Project project = projectService.getProjectById(projectId);

        BigDecimal totalPurchaseOrderAmount = purchaseOrderRepository.findByProjectId(projectId)
                .stream()
                .map(PurchaseOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<UUID, ProjectCostAccumulator> byProduct = new LinkedHashMap<>();
        materialIssueRepository.findByProjectId(projectId)
                .forEach(issue -> materialIssueItemRepository.findByMaterialIssueId(issue.getId())
                        .forEach(item -> byProduct.computeIfAbsent(item.getProductId(), ignored -> new ProjectCostAccumulator())
                                .add(item.getQuantity(), item.getLineTotal())));

        List<ProjectCostItemDto> items = byProduct.entrySet()
                .stream()
                .map(entry -> new ProjectCostItemDto(
                        entry.getKey(),
                        productRepository.findById(entry.getKey()).map(Product::getName).orElse("Unknown Product"),
                        entry.getValue().quantity,
                        entry.getValue().cost))
                .toList();

        BigDecimal totalMaterialIssuedCost = items.stream()
                .map(ProjectCostItemDto::getIssuedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ProjectCostSummaryDto(
                project.getId(),
                project.getProjectName(),
                totalPurchaseOrderAmount,
                totalMaterialIssuedCost,
                items);
    }

    private static final class ProjectCostAccumulator {
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal cost = BigDecimal.ZERO;

        private void add(BigDecimal additionalQuantity, BigDecimal additionalCost) {
            quantity = quantity.add(additionalQuantity);
            cost = cost.add(additionalCost);
        }
    }
}
