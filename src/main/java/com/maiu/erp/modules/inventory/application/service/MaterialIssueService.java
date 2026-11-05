package com.maiu.erp.modules.inventory.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.application.dto.CreateMaterialIssueItemRequest;
import com.maiu.erp.modules.inventory.application.dto.CreateMaterialIssueRequest;
import com.maiu.erp.modules.inventory.application.dto.CreateMaterialReturnItemRequest;
import com.maiu.erp.modules.inventory.application.dto.CreateMaterialReturnRequest;
import com.maiu.erp.modules.inventory.application.dto.ReverseMaterialIssueRequest;
import com.maiu.erp.modules.inventory.domain.model.MaterialIssue;
import com.maiu.erp.modules.inventory.domain.model.MaterialIssueItem;
import com.maiu.erp.modules.inventory.domain.model.MaterialReturn;
import com.maiu.erp.modules.inventory.domain.model.MaterialReturnItem;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialReturnItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialReturnRepository;
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
    private final MaterialReturnRepository materialReturnRepository;
    private final MaterialReturnItemRepository materialReturnItemRepository;
    private final InventoryService inventoryService;
    private final InventoryValidationService inventoryValidationService;
    private final ProjectRepository projectRepository;

    public MaterialIssueService(
            MaterialIssueRepository materialIssueRepository,
            MaterialIssueItemRepository materialIssueItemRepository,
            MaterialReturnRepository materialReturnRepository,
            MaterialReturnItemRepository materialReturnItemRepository,
            InventoryService inventoryService,
            InventoryValidationService inventoryValidationService,
            ProjectRepository projectRepository) {
        this.materialIssueRepository = materialIssueRepository;
        this.materialIssueItemRepository = materialIssueItemRepository;
        this.materialReturnRepository = materialReturnRepository;
        this.materialReturnItemRepository = materialReturnItemRepository;
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
        String performedBy = normalizeRequiredText(request.performedBy(), "performedBy is required");

        MaterialIssue materialIssue = new MaterialIssue();
        materialIssue.setIssueNumber(generateIssueNumber());
        materialIssue.setProjectId(request.projectId());
        materialIssue.setWarehouseId(request.warehouseId());
        materialIssue.setRemarks(trimToNull(request.remarks()));
        materialIssue.setPerformedBy(performedBy);
        materialIssue.setIssuedAt(Instant.now());

        MaterialIssue savedIssue = materialIssueRepository.save(materialIssue);

        for (CreateMaterialIssueItemRequest itemRequest : request.items()) {
            validateItem(itemRequest, request.warehouseId());

            // Inventory decides the cost (warehouse average); the requested cost is only a fallback.
            BigDecimal issuedUnitCost = inventoryService.issueStockToProject(
                    itemRequest.productId(),
                    request.warehouseId(),
                    request.projectId(),
                    itemRequest.quantity(),
                    itemRequest.unitCost(),
                    savedIssue.getId(),
                    savedIssue.getRemarks(),
                    savedIssue.getPerformedBy());
            if (issuedUnitCost == null) {
                issuedUnitCost = itemRequest.unitCost() == null ? BigDecimal.ZERO : itemRequest.unitCost();
            }

            MaterialIssueItem item = new MaterialIssueItem();
            item.setMaterialIssueId(savedIssue.getId());
            item.setProductId(itemRequest.productId());
            item.setQuantity(itemRequest.quantity());
            item.setUnitCost(issuedUnitCost);
            item.setLineTotal(itemRequest.quantity().multiply(issuedUnitCost));
            materialIssueItemRepository.save(item);
        }

        return savedIssue.getId();
    }

    @Transactional
    public UUID createMaterialReturn(UUID issueId, CreateMaterialReturnRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Material return must have at least one item");
        }

        MaterialIssue issue = getMaterialIssueById(issueId);
        String performedBy = normalizeRequiredText(request.performedBy(), "performedBy is required");
        List<MaterialIssueItem> issuedItems = materialIssueItemRepository.findByMaterialIssueId(issueId);
        if (issuedItems.isEmpty()) {
            throw new BadRequestException("Material issue has no items");
        }

        Map<UUID, IssueBalance> issuedByProduct = buildIssuedBalances(issuedItems);
        Map<UUID, QuantityCostAccumulator> returnedByProduct = buildReturnedBalances(issueId);

        MaterialReturn savedReturn = createReturnHeader(
                issueId,
                issue,
                trimToNull(request.remarks()),
                performedBy,
                false);

        for (CreateMaterialReturnItemRequest itemRequest : request.items()) {
            validateReturnItem(itemRequest);
            IssueBalance issueBalance = issuedByProduct.get(itemRequest.productId());
            if (issueBalance == null) {
                throw new BadRequestException("Returned product is not part of the source material issue");
            }

            QuantityCostAccumulator alreadyReturned = returnedByProduct.computeIfAbsent(
                    itemRequest.productId(),
                    ignored -> new QuantityCostAccumulator());
            if (alreadyReturned.quantity.add(itemRequest.quantity()).compareTo(issueBalance.quantity()) > 0) {
                throw new BadRequestException("Return quantity exceeds issued quantity for product");
            }
            BigDecimal lineTotal = calculateReturnLineTotal(issueBalance, alreadyReturned, itemRequest.quantity());
            alreadyReturned.add(itemRequest.quantity(), lineTotal);

            MaterialReturnItem item = new MaterialReturnItem();
            item.setMaterialReturnId(savedReturn.getId());
            item.setProductId(itemRequest.productId());
            item.setQuantity(itemRequest.quantity());
            item.setUnitCost(issueBalance.unitCost());
            item.setLineTotal(lineTotal);
            materialReturnItemRepository.save(item);

            inventoryService.returnStockFromProject(
                    itemRequest.productId(),
                    issue.getWarehouseId(),
                    issue.getProjectId(),
                    itemRequest.quantity(),
                    issueBalance.unitCost(),
                    savedReturn.getId(),
                    savedReturn.getRemarks(),
                    savedReturn.getPerformedBy());
        }

        return savedReturn.getId();
    }

    @Transactional
    public UUID reverseMaterialIssue(UUID issueId, ReverseMaterialIssueRequest request) {
        MaterialIssue issue = getMaterialIssueById(issueId);
        String performedBy = normalizeRequiredText(request.performedBy(), "performedBy is required");
        List<MaterialIssueItem> issuedItems = materialIssueItemRepository.findByMaterialIssueId(issueId);
        if (issuedItems.isEmpty()) {
            throw new BadRequestException("Material issue has no items");
        }

        Map<UUID, IssueBalance> issuedByProduct = buildIssuedBalances(issuedItems);
        Map<UUID, QuantityCostAccumulator> returnedByProduct = buildReturnedBalances(issueId);
        Map<UUID, BigDecimal> remainingByProduct = buildRemainingQuantities(issuedByProduct, returnedByProduct);

        if (remainingByProduct.values().stream().allMatch(quantity -> quantity.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BadRequestException("Material issue is already fully reversed or returned");
        }

        String remarks = buildReversalRemarks(trimToNull(request.remarks()), issue.getIssueNumber());
        MaterialReturn savedReturn = createReturnHeader(issueId, issue, remarks, performedBy, true);

        remainingByProduct.forEach((productId, remainingQuantity) -> {
            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }

            IssueBalance issueBalance = issuedByProduct.get(productId);
            QuantityCostAccumulator alreadyReturned = returnedByProduct.getOrDefault(productId, new QuantityCostAccumulator());
            MaterialReturnItem item = new MaterialReturnItem();
            item.setMaterialReturnId(savedReturn.getId());
            item.setProductId(productId);
            item.setQuantity(remainingQuantity);
            item.setUnitCost(issueBalance.unitCost());
            item.setLineTotal(calculateReturnLineTotal(issueBalance, alreadyReturned, remainingQuantity));
            materialReturnItemRepository.save(item);

            inventoryService.returnStockFromProject(
                    productId,
                    issue.getWarehouseId(),
                    issue.getProjectId(),
                    remainingQuantity,
                    issueBalance.unitCost(),
                    savedReturn.getId(),
                    savedReturn.getRemarks(),
                    savedReturn.getPerformedBy());
        });

        return savedReturn.getId();
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

    public List<MaterialReturn> getMaterialReturns() {
        return materialReturnRepository.findAll();
    }

    public MaterialReturn getMaterialReturnById(UUID returnId) {
        return materialReturnRepository.findById(returnId)
                .orElseThrow(() -> new NotFoundException("Material return not found"));
    }

    public List<MaterialReturnItem> getMaterialReturnItems(UUID returnId) {
        return materialReturnItemRepository.findByMaterialReturnId(returnId);
    }

    private void validateItem(CreateMaterialIssueItemRequest itemRequest, UUID warehouseId) {
        if (itemRequest.productId() == null) {
            throw new BadRequestException("Product ID is required");
        }
        if (itemRequest.quantity() == null || itemRequest.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Item quantity must be greater than zero");
        }
        if (itemRequest.unitCost() != null && itemRequest.unitCost().compareTo(BigDecimal.ZERO) < 0) {
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

    private String generateReturnNumber() {
        String returnNumber;
        do {
            returnNumber = "MR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (materialReturnRepository.findByReturnNumber(returnNumber).isPresent());

        return returnNumber;
    }

    private MaterialReturn createReturnHeader(
            UUID issueId,
            MaterialIssue issue,
            String remarks,
            String performedBy,
            boolean reversal) {
        MaterialReturn materialReturn = new MaterialReturn();
        materialReturn.setReturnNumber(generateReturnNumber());
        materialReturn.setMaterialIssueId(issueId);
        materialReturn.setProjectId(issue.getProjectId());
        materialReturn.setWarehouseId(issue.getWarehouseId());
        materialReturn.setReversal(reversal);
        materialReturn.setRemarks(remarks);
        materialReturn.setPerformedBy(performedBy);
        materialReturn.setReturnedAt(Instant.now());
        return materialReturnRepository.save(materialReturn);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }

        return value.trim();
    }

    private void validateReturnItem(CreateMaterialReturnItemRequest itemRequest) {
        if (itemRequest.productId() == null) {
            throw new BadRequestException("Product ID is required");
        }
        if (itemRequest.quantity() == null || itemRequest.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Item quantity must be greater than zero");
        }
    }

    private Map<UUID, IssueBalance> buildIssuedBalances(List<MaterialIssueItem> issuedItems) {
        Map<UUID, QuantityCostAccumulator> accumulators = new LinkedHashMap<>();
        for (MaterialIssueItem issuedItem : issuedItems) {
            accumulators.computeIfAbsent(issuedItem.getProductId(), ignored -> new QuantityCostAccumulator())
                    .add(issuedItem.getQuantity(), issuedItem.getLineTotal());
        }

        Map<UUID, IssueBalance> balances = new LinkedHashMap<>();
        accumulators.forEach((productId, accumulator) -> balances.put(productId, accumulator.toBalance()));
        return balances;
    }

    private Map<UUID, QuantityCostAccumulator> buildReturnedBalances(UUID issueId) {
        Map<UUID, QuantityCostAccumulator> returnedByProduct = new LinkedHashMap<>();
        for (MaterialReturn materialReturn : materialReturnRepository.findByMaterialIssueId(issueId)) {
            for (MaterialReturnItem item : materialReturnItemRepository.findByMaterialReturnId(materialReturn.getId())) {
                returnedByProduct.computeIfAbsent(item.getProductId(), ignored -> new QuantityCostAccumulator())
                        .add(item.getQuantity(), item.getLineTotal());
            }
        }
        return returnedByProduct;
    }

    private Map<UUID, BigDecimal> buildRemainingQuantities(
            Map<UUID, IssueBalance> issuedByProduct,
            Map<UUID, QuantityCostAccumulator> returnedByProduct) {
        Map<UUID, BigDecimal> remainingByProduct = new LinkedHashMap<>();
        issuedByProduct.forEach((productId, issueBalance) -> {
            QuantityCostAccumulator returned = returnedByProduct.get(productId);
            BigDecimal returnedQuantity = returned == null ? BigDecimal.ZERO : returned.quantity;
            remainingByProduct.put(productId, issueBalance.quantity().subtract(returnedQuantity));
        });
        return remainingByProduct;
    }

    /**
     * Prices a return so that cumulative returned cost tracks the issued line total exactly:
     * the cost of the returned share is computed from the original total rather than from a
     * rounded unit cost, and the return that clears the balance refunds whatever cost remains.
     */
    private BigDecimal calculateReturnLineTotal(
            IssueBalance issueBalance,
            QuantityCostAccumulator alreadyReturned,
            BigDecimal returnQuantity) {
        BigDecimal newReturnedQuantity = alreadyReturned.quantity.add(returnQuantity);
        BigDecimal cumulativeReturnedCost;
        if (newReturnedQuantity.compareTo(issueBalance.quantity()) >= 0) {
            cumulativeReturnedCost = issueBalance.lineTotal();
        } else {
            int scale = Math.max(2, issueBalance.lineTotal().scale());
            cumulativeReturnedCost = issueBalance.lineTotal()
                    .multiply(newReturnedQuantity)
                    .divide(issueBalance.quantity(), scale, RoundingMode.HALF_UP);
        }
        return cumulativeReturnedCost.subtract(alreadyReturned.lineTotal);
    }

    private String buildReversalRemarks(String remarks, String issueNumber) {
        String prefix = "Reversal of " + issueNumber;
        if (remarks == null) {
            return prefix;
        }
        return prefix + " | " + remarks;
    }

    private record IssueBalance(BigDecimal quantity, BigDecimal unitCost, BigDecimal lineTotal) {
    }

    private static final class QuantityCostAccumulator {
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal lineTotal = BigDecimal.ZERO;

        private void add(BigDecimal additionalQuantity, BigDecimal additionalLineTotal) {
            quantity = quantity.add(additionalQuantity);
            lineTotal = lineTotal.add(additionalLineTotal);
        }

        private IssueBalance toBalance() {
            BigDecimal unitCost = quantity.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : lineTotal.divide(quantity, 2, RoundingMode.HALF_UP);
            return new IssueBalance(quantity, unitCost, lineTotal);
        }
    }
}
