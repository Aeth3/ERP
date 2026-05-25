package com.maiu.erp.modules.inventory.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.inventory.application.dto.StockMovementDto;
import com.maiu.erp.modules.inventory.application.service.InventoryService;
import com.maiu.erp.modules.inventory.domain.model.StockMovement;

@RestController
@RequestMapping("/inventory/stock-movements")
public class StockMovementController {

    private final InventoryService inventoryService;

    public StockMovementController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<StockMovementDto>> getStockMovements(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) UUID referenceId,
            @RequestParam(required = false) String referenceType) {
        return ResponseEntity.ok(
                inventoryService.getStockMovements(productId, warehouseId, referenceId, referenceType).stream()
                        .map(this::toDto)
                        .toList());
    }

    private StockMovementDto toDto(StockMovement movement) {
        return new StockMovementDto(
                movement.getId(),
                movement.getProductId(),
                movement.getWarehouseId(),
                movement.getProjectId(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getUnitCost(),
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getRemarks(),
                movement.getPerformedBy(),
                movement.getMovementDate());
    }
}
