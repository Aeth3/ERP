package com.maiu.erp.modules.inventory.application.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.repository.InventoryStockRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.WarehouseRepository;

@Service
public class InventoryValidationService {
    private final InventoryStockRepository inventoryStockRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    public InventoryValidationService(
            InventoryStockRepository inventoryStockRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository) {
        this.inventoryStockRepository = inventoryStockRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
    }

    public void validateAvailableStock(
            UUID productId,
            UUID warehouseId,
            BigDecimal requiredQuantity) {
        validateWarehouseExists(warehouseId);
        if (requiredQuantity == null
                || requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Required quantity must be greater than zero");
        }

        InventoryStock stock = inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new RuntimeException("Stock not found"));

        BigDecimal availableQuantity = stock.getQuantityOnHand()
                .subtract(stock.getReservedQuantity());

        if (availableQuantity.compareTo(requiredQuantity) < 0) {
            throw new RuntimeException("Insufficient available stock");
        }
    }

    public void validateWarehouseExists(
            UUID warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse ID is required");
        }
        warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
    }

    public void validateProductActive(
            UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new RuntimeException("Product is inactive");
        }
    }
}
