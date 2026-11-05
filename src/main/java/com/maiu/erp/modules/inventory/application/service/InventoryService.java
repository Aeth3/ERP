package com.maiu.erp.modules.inventory.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.application.port.InventoryLedgerEvent;
import com.maiu.erp.modules.inventory.application.port.InventoryLedgerPoster;
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

    private static final int COST_SCALE = 4;

    private final InventoryStockRepository inventoryStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryValidationService inventoryValidationService;
    private final InventoryLedgerPoster inventoryLedgerPoster;

    public InventoryService(
            InventoryStockRepository inventoryStockRepository,
            StockMovementRepository stockMovementRepository,
            InventoryValidationService inventoryValidationService,
            InventoryLedgerPoster inventoryLedgerPoster) {

        this.inventoryStockRepository = inventoryStockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.inventoryValidationService = inventoryValidationService;
        this.inventoryLedgerPoster = inventoryLedgerPoster;
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
        validateWarehouseAndActor(warehouseId, performedBy);
        String normalizedPerformedBy = performedBy.trim();
        validatePositiveQuantity(quantity);

        InventoryStock stock = getOrCreateStock(productId, warehouseId);

        applyInboundCost(stock, quantity, unitCost);
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
        movement.setPerformedBy(normalizedPerformedBy);
        movement.setMovementDate(Instant.now());

        postToLedger(InventoryLedgerEvent.Kind.PURCHASE_RECEIPT, stockMovementRepository.save(movement));
    }

    /** Ships stock out for a sale. The movement is valued at average cost; the price stays on the order line. */
    @Transactional
    public void decreaseStock(
            UUID productId,
            UUID warehouseId,
            BigDecimal quantity,
            String referenceType,
            UUID referenceId,
            String performedBy) {
        validateWarehouseAndActor(warehouseId, performedBy);
        String normalizedPerformedBy = performedBy.trim();
        validatePositiveQuantity(quantity);
        InventoryStock stock = inventoryStockRepository
                .findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                .orElseThrow(() -> new NotFoundException("Stock not found"));

        if (stock.getQuantityOnHand().compareTo(quantity) < 0) {
            throw new BadRequestException("Insufficient stock");
        }

        BigDecimal reservedToConsume = stock.getReservedQuantity()
                .min(quantity);
        BigDecimal shipmentUnitCost = currentAverageCost(stock);

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
        movement.setUnitCost(shipmentUnitCost);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setPerformedBy(normalizedPerformedBy);
        movement.setMovementDate(Instant.now());
        postToLedger(InventoryLedgerEvent.Kind.SALES_SHIPMENT, stockMovementRepository.save(movement));
    }

    @Transactional
    public void reserveStock(
            UUID productId,
            UUID warehouseId,
            BigDecimal quantity) {
        validatePositiveQuantity(quantity);

        InventoryStock stock = inventoryStockRepository
                .findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
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
                .findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
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

    public List<InventoryStock> getAllStocks() {
        return inventoryStockRepository.findAll();
    }

    public List<StockMovement> getStockMovements(
            UUID productId,
            UUID warehouseId,
            UUID projectId,
            UUID referenceId,
            String referenceType,
            String movementType,
            LocalDate startDate,
            LocalDate endDate) {
        if (referenceId == null
                && productId == null
                && warehouseId == null
                && projectId == null
                && (referenceType == null || referenceType.isBlank())
                && (movementType == null || movementType.isBlank())
                && startDate == null
                && endDate == null) {
            throw new BadRequestException(
                    "Provide productId, warehouseId, projectId, referenceId, referenceType, movementType, or a date range");
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate cannot be after endDate");
        }

        List<StockMovement> movements;
        String normalizedReferenceType = referenceType == null
                ? null
                : referenceType.trim().toUpperCase();
        String normalizedMovementType = normalizeMovementType(movementType);
        Instant rangeStart = startDate == null ? null : startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant rangeEndExclusive = endDate == null ? null : endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        if (referenceId != null) {
            movements = stockMovementRepository.findByReferenceId(referenceId);
        } else if (productId != null) {
            movements = stockMovementRepository.findByProductId(productId);
        } else if (warehouseId != null) {
            movements = stockMovementRepository.findByWarehouseId(warehouseId);
        } else if (projectId != null) {
            movements = stockMovementRepository.findByProjectId(projectId);
        } else if (rangeStart != null && rangeEndExclusive != null) {
            movements = stockMovementRepository.findBetweenDates(rangeStart, rangeEndExclusive.minusNanos(1));
        } else {
            movements = stockMovementRepository.findAll();
        }

        return movements.stream()
                .filter(movement -> productId == null || productId.equals(movement.getProductId()))
                .filter(movement -> warehouseId == null || warehouseId.equals(movement.getWarehouseId()))
                .filter(movement -> projectId == null || projectId.equals(movement.getProjectId()))
                .filter(movement -> referenceId == null || referenceId.equals(movement.getReferenceId()))
                .filter(movement -> normalizedReferenceType == null
                        || normalizedReferenceType.equalsIgnoreCase(movement.getReferenceType()))
                .filter(movement -> normalizedMovementType == null
                        || normalizedMovementType.equalsIgnoreCase(movement.getMovementType().name()))
                .filter(movement -> rangeStart == null || !movement.getMovementDate().isBefore(rangeStart))
                .filter(movement -> rangeEndExclusive == null || movement.getMovementDate().isBefore(rangeEndExclusive))
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
        validateWarehouseAndActor(warehouseId, performedBy);
        String normalizedPerformedBy = performedBy.trim();
        validatePositiveQuantity(quantity);
        inventoryValidationService.validateProductActive(productId);

        String normalizedAdjustmentType = normalizeAdjustmentType(adjustmentType);
        InventoryStock stock = getOrCreateStock(productId, warehouseId);
        BigDecimal effectiveUnitCost;

        if ("DECREASE".equals(normalizedAdjustmentType)) {
            BigDecimal availableQuantity = stock.getQuantityOnHand()
                    .subtract(stock.getReservedQuantity());

            if (availableQuantity.compareTo(quantity) < 0) {
                throw new BadRequestException("Insufficient available stock");
            }

            effectiveUnitCost = currentAverageCost(stock);
            stock.setQuantityOnHand(
                    stock.getQuantityOnHand().subtract(quantity));
        } else {
            // A counted-in increase uses the entered cost when given, otherwise the running average.
            effectiveUnitCost = unitCost != null && unitCost.signum() > 0
                    ? unitCost
                    : currentAverageCost(stock);
            applyInboundCost(stock, quantity, effectiveUnitCost);
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
        movement.setUnitCost(effectiveUnitCost);
        movement.setReferenceType("STOCK_ADJUSTMENT");
        movement.setRemarks(formatAdjustmentRemarks(normalizedAdjustmentType, reason, notes));
        movement.setPerformedBy(normalizedPerformedBy);
        movement.setMovementDate(Instant.now());

        postToLedger(
                "DECREASE".equals(normalizedAdjustmentType)
                        ? InventoryLedgerEvent.Kind.ADJUSTMENT_DECREASE
                        : InventoryLedgerEvent.Kind.ADJUSTMENT_INCREASE,
                stockMovementRepository.save(movement));
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
        validateWarehouseAndActor(fromWarehouseId, performedBy);
        String normalizedPerformedBy = performedBy.trim();
        validatePositiveQuantity(quantity);
        inventoryValidationService.validateProductActive(productId);
        inventoryValidationService.validateWarehouseExists(toWarehouseId);

        if (fromWarehouseId.equals(toWarehouseId)) {
            throw new BadRequestException("Source and destination warehouses must be different");
        }

        // Lock both rows in a fixed order so opposite transfers (A->B, B->A) cannot deadlock.
        InventoryStock sourceStock;
        InventoryStock destinationStock;
        if (fromWarehouseId.compareTo(toWarehouseId) < 0) {
            sourceStock = getStockForUpdate(productId, fromWarehouseId);
            destinationStock = getOrCreateStock(productId, toWarehouseId);
        } else {
            destinationStock = getOrCreateStock(productId, toWarehouseId);
            sourceStock = getStockForUpdate(productId, fromWarehouseId);
        }

        BigDecimal sourceAvailable = sourceStock.getQuantityOnHand()
                .subtract(sourceStock.getReservedQuantity());

        if (sourceAvailable.compareTo(quantity) < 0) {
            throw new BadRequestException("Insufficient available stock");
        }

        // Stock moves between warehouses at the source's average cost.
        BigDecimal transferUnitCost = resolveOutboundCost(sourceStock, unitCost);

        sourceStock.setQuantityOnHand(sourceStock.getQuantityOnHand().subtract(quantity));
        sourceStock.setUpdatedAt(Instant.now());

        applyInboundCost(destinationStock, quantity, transferUnitCost);
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
                transferUnitCost,
                transferReferenceId,
                transferRemarks,
                normalizedPerformedBy,
                "OUT"));

        stockMovementRepository.save(createTransferMovement(
                productId,
                toWarehouseId,
                null,
                quantity,
                transferUnitCost,
                transferReferenceId,
                transferRemarks,
                normalizedPerformedBy,
                "IN"));
    }

    /**
     * Issues stock to a project at the warehouse's average cost and returns that cost.
     * The given unitCost is only used when the stock has no average cost yet.
     */
    @Transactional
    public BigDecimal issueStockToProject(
            UUID productId,
            UUID warehouseId,
            UUID projectId,
            BigDecimal quantity,
            BigDecimal unitCost,
            UUID referenceId,
            String remarks,
            String performedBy) {
        validateWarehouseAndActor(warehouseId, performedBy);
        String normalizedPerformedBy = performedBy.trim();
        validatePositiveQuantity(quantity);

        InventoryStock stock = inventoryStockRepository
                .findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                .orElseThrow(() -> new NotFoundException("Stock not found"));

        BigDecimal availableQuantity = stock.getQuantityOnHand()
                .subtract(stock.getReservedQuantity());

        if (availableQuantity.compareTo(quantity) < 0) {
            throw new BadRequestException("Insufficient available stock");
        }

        BigDecimal issueUnitCost = resolveOutboundCost(stock, unitCost)
                .setScale(2, RoundingMode.HALF_UP);

        stock.setQuantityOnHand(stock.getQuantityOnHand().subtract(quantity));
        stock.setUpdatedAt(Instant.now());
        inventoryStockRepository.save(stock);

        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setWarehouseId(warehouseId);
        movement.setProjectId(projectId);
        movement.setMovementType(MovementType.PROJECT_ISSUE);
        movement.setQuantity(quantity);
        movement.setUnitCost(issueUnitCost);
        movement.setReferenceType("MATERIAL_ISSUE");
        movement.setReferenceId(referenceId);
        movement.setRemarks(remarks);
        movement.setPerformedBy(normalizedPerformedBy);
        movement.setMovementDate(Instant.now());
        postToLedger(InventoryLedgerEvent.Kind.PROJECT_ISSUE, stockMovementRepository.save(movement));
        return issueUnitCost;
    }

    @Transactional
    public void returnStockFromProject(
            UUID productId,
            UUID warehouseId,
            UUID projectId,
            BigDecimal quantity,
            BigDecimal unitCost,
            UUID referenceId,
            String remarks,
            String performedBy) {
        validateWarehouseAndActor(warehouseId, performedBy);
        String normalizedPerformedBy = performedBy.trim();
        validatePositiveQuantity(quantity);

        InventoryStock stock = getOrCreateStock(productId, warehouseId);
        applyInboundCost(stock, quantity, unitCost);
        stock.setQuantityOnHand(stock.getQuantityOnHand().add(quantity));
        stock.setUpdatedAt(Instant.now());
        inventoryStockRepository.save(stock);

        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setWarehouseId(warehouseId);
        movement.setProjectId(projectId);
        movement.setMovementType(MovementType.RETURN);
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost == null ? BigDecimal.ZERO : unitCost.setScale(2, RoundingMode.HALF_UP));
        movement.setReferenceType("MATERIAL_RETURN");
        movement.setReferenceId(referenceId);
        movement.setRemarks(remarks);
        movement.setPerformedBy(normalizedPerformedBy);
        movement.setMovementDate(Instant.now());
        postToLedger(InventoryLedgerEvent.Kind.PROJECT_RETURN, stockMovementRepository.save(movement));
    }

    @Transactional
    public void returnPurchaseStock(
            UUID productId,
            UUID warehouseId,
            BigDecimal quantity,
            BigDecimal unitCost,
            UUID referenceId,
            String remarks,
            String performedBy) {
        validateWarehouseAndActor(warehouseId, performedBy);
        String normalizedPerformedBy = performedBy.trim();
        validatePositiveQuantity(quantity);

        InventoryStock stock = inventoryStockRepository
                .findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                .orElseThrow(() -> new NotFoundException("Stock not found"));

        BigDecimal availableQuantity = stock.getQuantityOnHand().subtract(stock.getReservedQuantity());
        if (availableQuantity.compareTo(quantity) < 0) {
            throw new BadRequestException("Insufficient available stock");
        }

        applyOutboundAtCost(stock, quantity, unitCost);
        stock.setQuantityOnHand(stock.getQuantityOnHand().subtract(quantity));
        stock.setUpdatedAt(Instant.now());
        inventoryStockRepository.save(stock);

        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setWarehouseId(warehouseId);
        movement.setProjectId(null);
        movement.setMovementType(MovementType.RETURN);
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost == null ? BigDecimal.ZERO : unitCost.setScale(2, RoundingMode.HALF_UP));
        movement.setReferenceType("PURCHASE_RETURN");
        movement.setReferenceId(referenceId);
        movement.setRemarks(remarks);
        movement.setPerformedBy(normalizedPerformedBy);
        movement.setMovementDate(Instant.now());
        postToLedger(InventoryLedgerEvent.Kind.PURCHASE_RETURN, stockMovementRepository.save(movement));
    }

    /** Takes sold stock back in at the current average cost; the average itself is unchanged. */
    @Transactional
    public void receiveSalesReturn(
            UUID productId,
            UUID warehouseId,
            BigDecimal quantity,
            UUID referenceId,
            String remarks,
            String performedBy) {
        validateWarehouseAndActor(warehouseId, performedBy);
        String normalizedPerformedBy = performedBy.trim();
        validatePositiveQuantity(quantity);

        InventoryStock stock = getOrCreateStock(productId, warehouseId);
        stock.setQuantityOnHand(stock.getQuantityOnHand().add(quantity));
        stock.setUpdatedAt(Instant.now());
        inventoryStockRepository.save(stock);

        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setWarehouseId(warehouseId);
        movement.setProjectId(null);
        movement.setMovementType(MovementType.RETURN);
        movement.setQuantity(quantity);
        movement.setUnitCost(currentAverageCost(stock));
        movement.setReferenceType("SALES_RETURN");
        movement.setReferenceId(referenceId);
        movement.setRemarks(remarks);
        movement.setPerformedBy(normalizedPerformedBy);
        movement.setMovementDate(Instant.now());
        postToLedger(InventoryLedgerEvent.Kind.SALES_RETURN, stockMovementRepository.save(movement));
    }

    private InventoryStock getStockForUpdate(
            UUID productId,
            UUID warehouseId) {
        return inventoryStockRepository
                .findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                .orElseThrow(() -> new NotFoundException("Stock not found"));
    }

    private InventoryStock getOrCreateStock(
            UUID productId,
            UUID warehouseId) {
        return inventoryStockRepository
                .findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                .orElseGet(() -> {
                    InventoryStock newStock = new InventoryStock();
                    newStock.setProductId(productId);
                    newStock.setWarehouseId(warehouseId);
                    newStock.setQuantityOnHand(BigDecimal.ZERO);
                    newStock.setReservedQuantity(BigDecimal.ZERO);
                    newStock.setReorderLevel(BigDecimal.ZERO);
                    newStock.setAverageCost(BigDecimal.ZERO);
                    newStock.setUpdatedAt(Instant.now());
                    return newStock;
                });
    }

    private void postToLedger(InventoryLedgerEvent.Kind kind, StockMovement savedMovement) {
        inventoryLedgerPoster.post(new InventoryLedgerEvent(
                kind,
                savedMovement.getId(),
                savedMovement.getReferenceId(),
                savedMovement.getProductId(),
                savedMovement.getWarehouseId(),
                savedMovement.getProjectId(),
                savedMovement.getQuantity(),
                savedMovement.getUnitCost(),
                savedMovement.getRemarks(),
                savedMovement.getPerformedBy(),
                savedMovement.getMovementDate()));
    }

    private BigDecimal currentAverageCost(InventoryStock stock) {
        return stock.getAverageCost() == null ? BigDecimal.ZERO : stock.getAverageCost();
    }

    // Average cost when known; otherwise the caller's fallback cost (or zero).
    private BigDecimal resolveOutboundCost(InventoryStock stock, BigDecimal fallbackUnitCost) {
        BigDecimal averageCost = currentAverageCost(stock);
        if (averageCost.signum() > 0) {
            return averageCost;
        }
        return fallbackUnitCost == null || fallbackUnitCost.signum() < 0
                ? BigDecimal.ZERO
                : fallbackUnitCost;
    }

    // Blends incoming units into the weighted average. Call before quantityOnHand is increased.
    private void applyInboundCost(InventoryStock stock, BigDecimal quantity, BigDecimal unitCost) {
        if (unitCost == null || unitCost.signum() < 0) {
            return;
        }

        BigDecimal onHand = stock.getQuantityOnHand() == null ? BigDecimal.ZERO : stock.getQuantityOnHand();
        if (onHand.signum() <= 0) {
            stock.setAverageCost(unitCost.setScale(COST_SCALE, RoundingMode.HALF_UP));
            return;
        }

        BigDecimal totalValue = onHand.multiply(currentAverageCost(stock))
                .add(quantity.multiply(unitCost));
        stock.setAverageCost(totalValue.divide(onHand.add(quantity), COST_SCALE, RoundingMode.HALF_UP));
    }

    // Removes units at a specific cost (e.g. a purchase return at its PO price). Call before quantityOnHand is reduced.
    private void applyOutboundAtCost(InventoryStock stock, BigDecimal quantity, BigDecimal unitCost) {
        if (unitCost == null || unitCost.signum() < 0) {
            return;
        }

        BigDecimal remaining = stock.getQuantityOnHand().subtract(quantity);
        if (remaining.signum() <= 0) {
            return;
        }

        BigDecimal remainingValue = stock.getQuantityOnHand().multiply(currentAverageCost(stock))
                .subtract(quantity.multiply(unitCost))
                .max(BigDecimal.ZERO);
        stock.setAverageCost(remainingValue.divide(remaining, COST_SCALE, RoundingMode.HALF_UP));
    }

    private void validatePositiveQuantity(
            BigDecimal quantity) {
        if (quantity == null
                || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }
    }

    public void validateWarehouseAndActor(
            UUID warehouseId,
            String performedBy) {
        inventoryValidationService.validateWarehouseExists(warehouseId);

        if (performedBy == null || performedBy.isBlank()) {
            throw new BadRequestException("performedBy is required");
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

    private String normalizeMovementType(String movementType) {
        if (movementType == null || movementType.isBlank()) {
            return null;
        }

        String normalized = movementType.trim().toUpperCase();

        try {
            MovementType.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown movementType: " + movementType.trim());
        }
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
