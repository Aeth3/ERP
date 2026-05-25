package com.maiu.erp.modules.inventory.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.application.dto.CreateMaterialIssueItemRequest;
import com.maiu.erp.modules.inventory.application.dto.CreateMaterialIssueRequest;
import com.maiu.erp.modules.inventory.domain.model.MaterialIssue;
import com.maiu.erp.modules.inventory.domain.model.MaterialIssueItem;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueRepository;
import com.maiu.erp.modules.project.domain.enums.ProjectStatus;
import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.modules.project.domain.repository.ProjectRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;

@Service
public class MaterialIssueService {
    private final MaterialIssueRepository materialIssueRepository;
    private final MaterialIssueItemRepository materialIssueItemRepository;
    private final InventoryService inventoryService;
    private final InventoryValidationService inventoryValidationService;
    private final ProjectRepository projectRepository;

    public MaterialIssueService(
            MaterialIssueRepository materialIssueRepository,
            MaterialIssueItemRepository materialIssueItemRepository,
            InventoryService inventoryService,
            InventoryValidationService inventoryValidationService,
            ProjectRepository projectRepository) {
        this.materialIssueRepository = materialIssueRepository;
        this.materialIssueItemRepository = materialIssueItemRepository;
        this.inventoryService = inventoryService;
        this.inventoryValidationService = inventoryValidationService;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public UUID createMaterialIssue(CreateMaterialIssueRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Material issue must have at least one item");
        }

        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new NotFoundException("Project not found"));

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new BadRequestException("Project must be active");
        }

        inventoryValidationService.validateWarehouseExists(request.warehouseId());

        MaterialIssue materialIssue = new MaterialIssue();
        materialIssue.setIssueNumber(generateIssueNumber());
        materialIssue.setProjectId(request.projectId());
        materialIssue.setWarehouseId(request.warehouseId());
        materialIssue.setRemarks(trimToNull(request.remarks()));
        materialIssue.setPerformedBy(request.performedBy().trim());
        materialIssue.setIssuedAt(Instant.now());

        MaterialIssue savedIssue = materialIssueRepository.save(materialIssue);

        for (CreateMaterialIssueItemRequest itemRequest : request.items()) {
            validateItem(itemRequest, request.warehouseId());

            MaterialIssueItem item = new MaterialIssueItem();
            item.setMaterialIssueId(savedIssue.getId());
            item.setProductId(itemRequest.productId());
            item.setQuantity(itemRequest.quantity());
            item.setUnitCost(itemRequest.unitCost());
            item.setLineTotal(itemRequest.quantity().multiply(itemRequest.unitCost()));
            materialIssueItemRepository.save(item);

            inventoryService.issueStockToProject(
                    itemRequest.productId(),
                    request.warehouseId(),
                    request.projectId(),
                    itemRequest.quantity(),
                    itemRequest.unitCost(),
                    savedIssue.getId(),
                    savedIssue.getRemarks(),
                    savedIssue.getPerformedBy());
        }

        return savedIssue.getId();
    }

    public List<MaterialIssue> getMaterialIssues() {
        return materialIssueRepository.findAll();
    }

    public MaterialIssue getMaterialIssueById(UUID issueId) {
        return materialIssueRepository.findById(issueId)
                .orElseThrow(() -> new NotFoundException("Material issue not found"));
    }

    public List<MaterialIssueItem> getMaterialIssueItems(UUID issueId) {
        return materialIssueItemRepository.findByMaterialIssueId(issueId);
    }

    private void validateItem(CreateMaterialIssueItemRequest itemRequest, UUID warehouseId) {
        if (itemRequest.productId() == null) {
            throw new BadRequestException("Product ID is required");
        }
        if (itemRequest.quantity() == null || itemRequest.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Item quantity must be greater than zero");
        }
        if (itemRequest.unitCost() == null || itemRequest.unitCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Item unit cost must be zero or greater");
        }

        inventoryValidationService.validateProductActive(itemRequest.productId());
        inventoryValidationService.validateAvailableStock(itemRequest.productId(), warehouseId, itemRequest.quantity());
    }

    private String generateIssueNumber() {
        String issueNumber;
        do {
            issueNumber = "MI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (materialIssueRepository.findByIssueNumber(issueNumber).isPresent());

        return issueNumber;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
