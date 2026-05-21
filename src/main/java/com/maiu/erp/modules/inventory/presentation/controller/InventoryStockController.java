package com.maiu.erp.modules.inventory.presentation.controller;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.inventory.application.dto.InventoryStockDto;
import com.maiu.erp.modules.inventory.application.service.InventoryService;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;

@RestController
@RequestMapping("/inventory/stocks")
public class InventoryStockController {

    private final InventoryService inventoryService;

    public InventoryStockController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<?> getStocks(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID warehouseId) {
        if (productId != null && warehouseId != null) {
            return ResponseEntity.ok(
                    toDto(inventoryService.getStock(productId, warehouseId)));
        }

        if (productId != null) {
            return ResponseEntity.ok(
                    inventoryService.getStocksByProduct(productId).stream()
                            .map(this::toDto)
                            .toList());
        }

        if (warehouseId != null) {
            return ResponseEntity.ok(
                    inventoryService.getStocksByWarehouse(warehouseId).stream()
                            .map(this::toDto)
                            .toList());
        }

        throw new RuntimeException("Provide productId, warehouseId, or both");
    }

    private InventoryStockDto toDto(InventoryStock stock) {
        BigDecimal availableQuantity = stock.getQuantityOnHand()
                .subtract(stock.getReservedQuantity());

        return new InventoryStockDto(
                stock.getId(),
                stock.getProductId(),
                stock.getWarehouseId(),
                stock.getQuantityOnHand(),
                stock.getReservedQuantity(),
                availableQuantity,
                stock.getReorderLevel(),
                stock.getUpdatedAt());
    }
}
