package com.maiu.erp.modules.inventory.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.enums.MovementType;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.model.StockMovement;
import com.maiu.erp.modules.inventory.domain.repository.InventoryStockRepository;
import com.maiu.erp.modules.inventory.domain.repository.StockMovementRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;

@Service
public class InventoryService {

    private final InventoryStockRepository inventoryStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryValidationService inventoryValidationService;

    public InventoryService(
            InventoryStockRepository inventoryStockRepository,
            StockMovementRepository stockMovementRepository,
            InventoryValidationService inventoryValidationService) {

        this.inventoryStockRepository = inventoryStockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.inventoryValidationService = inventoryValidationService;
    }

    @Transactional
    public void increaseStock(
            UUID productId,
            UUID warehouseId,
            BigDecimal quantity,
            BigDecimal unitCost,
            String referenceType,
            UUID referenceId,
            String performedBy) {
        validatePositiveQuantity(quantity);

        InventoryStock stock = getOrCreateStock(productId, warehouseId);

        stock.setQuantityOnHand(
                stock.getQuantityOnHand().add(quantity));
        stock.setUpdatedAt(Instant.now());

        inventoryStockRepository.save(stock);

        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setWarehouseId(warehouseId);
        movement.setMovementType(MovementType.PURCHASE_IN);
        movement.setProjectId(null);
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setPerformedBy(performedBy);
        movement.setMovementDate(Instant.now());

        stockMovementRepository.save(movement);
    }

    @Transactional
    public void decreaseStock(
            UUID productId,
            UUID warehouseId,
            BigDecimal quantity,
            BigDecimal unitCost,
            String referenceType,
            UUID referenceId,
            String performedBy) {
        validatePositiveQuantity(quantity);
        InventoryStock stock = inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new NotFoundException("Stock not found"));

        if (stock.getQuantityOnHand().compareTo(quantity) < 0) {
            throw new BadRequestException("Insufficient stock");
        }

        BigDecimal reservedToConsume = stock.getReservedQuantity()
                .min(quantity);

        stock.setQuantityOnHand(
                stock.getQuantityOnHand().subtract(quantity));
        stock.setReservedQuantity(
                stock.getReservedQuantity().subtract(reservedToConsume));
        stock.setUpdatedAt(Instant.now());

        inventoryStockRepository.save(stock);

        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setWarehouseId(warehouseId);
        movement.setMovementType(MovementType.SALES_OUT);
        movement.setProjectId(null);
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setPerformedBy(performedBy);
        movement.setMovementDate(Instant.now());
        stockMovementRepository.save(movement);
    }

    @Transactional
    public void reserveStock(
            UUID productId,
            UUID warehouseId,
            BigDecimal quantity) {
        validatePositiveQuantity(quantity);

        InventoryStock stock = inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new NotFoundException("Stock not found"));

        BigDecimal availableQuantity = stock.getQuantityOnHand()
                .subtract(stock.getReservedQuantity());

        if (availableQuantity.compareTo(quantity) < 0) {
            throw new BadRequestException("Insufficient available stock");
        }

        stock.setReservedQuantity(
                stock.getReservedQuantity().add(quantity));
        stock.setUpdatedAt(Instant.now());

        inventoryStockRepository.save(stock);
    }

    @Transactional
    public void releaseReservedStock(
            UUID productId,
            UUID warehouseId,
            BigDecimal quantity) {
        validatePositiveQuantity(quantity);

        InventoryStock stock = inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new NotFoundException("Stock not found"));

        if (stock.getReservedQuantity().compareTo(quantity) < 0) {
            throw new BadRequestException("Reserved stock is lower than requested release");
        }

        stock.setReservedQuantity(
                stock.getReservedQuantity().subtract(quantity));
        stock.setUpdatedAt(Instant.now());

        inventoryStockRepository.save(stock);
    }

    public BigDecimal getAvailableStock(
            UUID productId,
            UUID warehouseId) {
        return inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .map(stock -> stock.getQuantityOnHand()
                        .subtract(stock.getReservedQuantity()))
                .orElse(BigDecimal.ZERO);
    }

    public InventoryStock getStock(
            UUID productId,
            UUID warehouseId) {
        return inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new NotFoundException("Stock not found"));
    }

    public Optional<InventoryStock> findStock(
            UUID productId,
            UUID warehouseId) {
        return inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId);
    }

    public List<InventoryStock> getStocksByProduct(
            UUID productId) {
        return inventoryStockRepository.findByProductId(productId);
    }

    public List<InventoryStock> getStocksByWarehouse(
            UUID warehouseId) {
        return inventoryStockRepository.findByWarehouseId(warehouseId);
    }

    public List<StockMovement> getStockMovements(
            UUID productId,
            UUID warehouseId,
            UUID referenceId,
            String referenceType) {
        if (referenceId == null
                && productId == null
                && warehouseId == null
                && (referenceType == null || referenceType.isBlank())) {
            throw new BadRequestException("Provide productId, warehouseId, referenceId, or referenceType");
        }

        List<StockMovement> movements;
        String normalizedReferenceType = referenceType == null
                ? null
                : referenceType.trim().toUpperCase();

        if (referenceId != null) {
            movements = stockMovementRepository.findByReferenceId(referenceId);
        } else if (productId != null) {
            movements = stockMovementRepository.findByProductId(productId);
        } else {
            movements = warehouseId != null
                    ? stockMovementRepository.findByWarehouseId(warehouseId)
                    : stockMovementRepository.findAll();
        }

        return movements.stream()
                .filter(movement -> productId == null || productId.equals(movement.getProductId()))
                .filter(movement -> warehouseId == null || warehouseId.equals(movement.getWarehouseId()))
                .filter(movement -> referenceId == null || referenceId.equals(movement.getReferenceId()))
                .filter(movement -> normalizedReferenceType == null
                        || normalizedReferenceType.equalsIgnoreCase(movement.getReferenceType()))
                .sorted(Comparator.comparing(StockMovement::getMovementDate).reversed())
                .toList();
    }

    @Transactional
    public void adjustStock(
            UUID productId,
            UUID warehouseId,
            String adjustmentType,
            BigDecimal quantity,
            BigDecimal unitCost,
            String reason,
            String notes,
            String performedBy) {
        validatePositiveQuantity(quantity);
        inventoryValidationService.validateProductActive(productId);
        inventoryValidationService.validateWarehouseExists(warehouseId);

        String normalizedAdjustmentType = normalizeAdjustmentType(adjustmentType);
        InventoryStock stock = getOrCreateStock(productId, warehouseId);

        if ("DECREASE".equals(normalizedAdjustmentType)) {
            BigDecimal availableQuantity = stock.getQuantityOnHand()
                    .subtract(stock.getReservedQuantity());

            if (availableQuantity.compareTo(quantity) < 0) {
                throw new BadRequestException("Insufficient available stock");
            }

            stock.setQuantityOnHand(
                    stock.getQuantityOnHand().subtract(quantity));
        } else {
            stock.setQuantityOnHand(
                    stock.getQuantityOnHand().add(quantity));
        }

        stock.setUpdatedAt(Instant.now());
        inventoryStockRepository.save(stock);

        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setWarehouseId(warehouseId);
        movement.setMovementType(MovementType.ADJUSTMENT);
        movement.setProjectId(null);
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost);
        movement.setReferenceType("STOCK_ADJUSTMENT");
        movement.setRemarks(formatAdjustmentRemarks(normalizedAdjustmentType, reason, notes));
        movement.setPerformedBy(performedBy);
        movement.setMovementDate(Instant.now());

        stockMovementRepository.save(movement);
    }

    @Transactional
    public void transferStock(
            UUID productId,
            UUID fromWarehouseId,
            UUID toWarehouseId,
            BigDecimal quantity,
            BigDecimal unitCost,
            String reason,
            String notes,
            String performedBy) {
        validatePositiveQuantity(quantity);
        inventoryValidationService.validateProductActive(productId);
        inventoryValidationService.validateWarehouseExists(fromWarehouseId);
        inventoryValidationService.validateWarehouseExists(toWarehouseId);

        if (fromWarehouseId.equals(toWarehouseId)) {
            throw new BadRequestException("Source and destination warehouses must be different");
        }

        InventoryStock sourceStock = inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, fromWarehouseId)
                .orElseThrow(() -> new NotFoundException("Stock not found"));

        BigDecimal sourceAvailable = sourceStock.getQuantityOnHand()
                .subtract(sourceStock.getReservedQuantity());

        if (sourceAvailable.compareTo(quantity) < 0) {
            throw new BadRequestException("Insufficient available stock");
        }

        InventoryStock destinationStock = getOrCreateStock(productId, toWarehouseId);

        sourceStock.setQuantityOnHand(sourceStock.getQuantityOnHand().subtract(quantity));
        sourceStock.setUpdatedAt(Instant.now());

        destinationStock.setQuantityOnHand(destinationStock.getQuantityOnHand().add(quantity));
        destinationStock.setUpdatedAt(Instant.now());

        inventoryStockRepository.save(sourceStock);
        inventoryStockRepository.save(destinationStock);

        UUID transferReferenceId = UUID.randomUUID();
        String transferRemarks = formatTransferRemarks(reason, notes);

        stockMovementRepository.save(createTransferMovement(
                productId,
                fromWarehouseId,
                null,
                quantity,
                unitCost,
                transferReferenceId,
                transferRemarks,
                performedBy,
                "OUT"));

        stockMovementRepository.save(createTransferMovement(
                productId,
                toWarehouseId,
                null,
                quantity,
                unitCost,
                transferReferenceId,
                transferRemarks,
                performedBy,
                "IN"));
    }

    @Transactional
    public void issueStockToProject(
            UUID productId,
            UUID warehouseId,
            UUID projectId,
            BigDecimal quantity,
            BigDecimal unitCost,
            UUID referenceId,
            String remarks,
            String performedBy) {
        validatePositiveQuantity(quantity);

        InventoryStock stock = inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new NotFoundException("Stock not found"));

        BigDecimal availableQuantity = stock.getQuantityOnHand()
                .subtract(stock.getReservedQuantity());

        if (availableQuantity.compareTo(quantity) < 0) {
            throw new BadRequestException("Insufficient available stock");
        }

        stock.setQuantityOnHand(stock.getQuantityOnHand().subtract(quantity));
        stock.setUpdatedAt(Instant.now());
        inventoryStockRepository.save(stock);

        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setWarehouseId(warehouseId);
        movement.setProjectId(projectId);
        movement.setMovementType(MovementType.PROJECT_ISSUE);
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost);
        movement.setReferenceType("MATERIAL_ISSUE");
        movement.setReferenceId(referenceId);
        movement.setRemarks(remarks);
        movement.setPerformedBy(performedBy);
        movement.setMovementDate(Instant.now());
        stockMovementRepository.save(movement);
    }

    private InventoryStock getOrCreateStock(
            UUID productId,
            UUID warehouseId) {
        return inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseGet(() -> {
                    InventoryStock newStock = new InventoryStock();
                    newStock.setProductId(productId);
                    newStock.setWarehouseId(warehouseId);
                    newStock.setQuantityOnHand(BigDecimal.ZERO);
                    newStock.setReservedQuantity(BigDecimal.ZERO);
                    newStock.setReorderLevel(BigDecimal.ZERO);
                    newStock.setUpdatedAt(Instant.now());
                    return newStock;
                });
    }

    private void validatePositiveQuantity(
            BigDecimal quantity) {
        if (quantity == null
                || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }
    }

    private String normalizeAdjustmentType(
            String adjustmentType) {
        if (adjustmentType == null || adjustmentType.isBlank()) {
            throw new BadRequestException("Adjustment type is required");
        }

        String normalized = adjustmentType.trim().toUpperCase();

        if (!"INCREASE".equals(normalized) && !"DECREASE".equals(normalized)) {
            throw new BadRequestException("Adjustment type must be INCREASE or DECREASE");
        }

        return normalized;
    }

    private String formatAdjustmentRemarks(
            String adjustmentType,
            String reason,
            String notes) {
        StringBuilder remarks = new StringBuilder(adjustmentType)
                .append(": ")
                .append(reason == null ? "" : reason.trim());

        if (notes != null && !notes.isBlank()) {
            remarks.append(" | ").append(notes.trim());
        }

        return remarks.toString();
    }

    private StockMovement createTransferMovement(
            UUID productId,
            UUID warehouseId,
            UUID projectId,
            BigDecimal quantity,
            BigDecimal unitCost,
            UUID referenceId,
            String remarks,
            String performedBy,
            String direction) {
        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setWarehouseId(warehouseId);
        movement.setProjectId(projectId);
        movement.setMovementType(MovementType.TRANSFER);
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost);
        movement.setReferenceType("STOCK_TRANSFER");
        movement.setReferenceId(referenceId);
        movement.setRemarks(direction + ": " + remarks);
        movement.setPerformedBy(performedBy);
        movement.setMovementDate(Instant.now());
        return movement;
    }

    private String formatTransferRemarks(
            String reason,
            String notes) {
        StringBuilder remarks = new StringBuilder(reason == null ? "" : reason.trim());

        if (notes != null && !notes.isBlank()) {
            remarks.append(" | ").append(notes.trim());
        }

        return remarks.toString();
    }
}
