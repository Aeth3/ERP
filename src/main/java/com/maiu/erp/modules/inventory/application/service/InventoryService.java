package com.maiu.erp.modules.inventory.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.enums.MovementType;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.model.StockMovement;
import com.maiu.erp.modules.inventory.domain.repository.InventoryStockRepository;
import com.maiu.erp.modules.inventory.domain.repository.StockMovementRepository;

import jakarta.transaction.Transactional;

@Service
public class InventoryService {

    private final InventoryStockRepository inventoryStockRepository;
    private final StockMovementRepository stockMovementRepository;

    public InventoryService(
            InventoryStockRepository inventoryStockRepository,
            StockMovementRepository stockMovementRepository) {

        this.inventoryStockRepository = inventoryStockRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional
    public void increaseStock(
            UUID productId,
            UUID warehouseId,
            BigDecimal quantity,
            BigDecimal unitCost,
            String referenceType,
            UUID referenceId,
            UUID performedBy) {
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
            UUID performedBy) {
        validatePositiveQuantity(quantity);
        InventoryStock stock = inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new RuntimeException("Stock not found"));

        if (stock.getQuantityOnHand().compareTo(quantity) < 0) {
            throw new RuntimeException("Insufficient stock");
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
                .orElseThrow(() -> new RuntimeException("Stock not found"));

        BigDecimal availableQuantity = stock.getQuantityOnHand()
                .subtract(stock.getReservedQuantity());

        if (availableQuantity.compareTo(quantity) < 0) {
            throw new RuntimeException("Insufficient available stock");
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
                .orElseThrow(() -> new RuntimeException("Stock not found"));

        if (stock.getReservedQuantity().compareTo(quantity) < 0) {
            throw new RuntimeException("Reserved stock is lower than requested release");
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
                .orElseThrow(() -> new RuntimeException("Stock not found"));
    }

    public List<InventoryStock> getStocksByProduct(
            UUID productId) {
        return inventoryStockRepository.findByProductId(productId);
    }

    public List<InventoryStock> getStocksByWarehouse(
            UUID warehouseId) {
        return inventoryStockRepository.findByWarehouseId(warehouseId);
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
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }
}
