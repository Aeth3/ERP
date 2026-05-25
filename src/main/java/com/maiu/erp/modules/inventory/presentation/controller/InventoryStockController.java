package com.maiu.erp.modules.inventory.presentation.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.inventory.application.dto.ActionResponse;
import com.maiu.erp.modules.inventory.application.dto.CreateStockAdjustmentRequest;
import com.maiu.erp.modules.inventory.application.dto.CreateStockTransferRequest;
import com.maiu.erp.modules.inventory.application.dto.InventoryStockDto;
import com.maiu.erp.modules.inventory.application.service.InventoryService;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.shared.exception.BadRequestException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/stocks")
public class InventoryStockController {

    private final InventoryService inventoryService;

    public InventoryStockController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<InventoryStockDto>> getStocks(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID warehouseId) {
        if (productId != null && warehouseId != null) {
            return ResponseEntity.ok(
                    inventoryService.findStock(productId, warehouseId)
                            .map(this::toDto)
                            .stream()
                            .toList());
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

        throw new BadRequestException("Provide productId, warehouseId, or both");
    }

    @PostMapping("/adjustments")
    public ResponseEntity<ActionResponse> createStockAdjustment(
            @Valid @RequestBody CreateStockAdjustmentRequest request) {
        inventoryService.adjustStock(
                request.productId(),
                request.warehouseId(),
                request.adjustmentType(),
                request.quantity(),
                request.unitCost(),
                request.reason(),
                request.notes(),
                request.performedBy());

        return ResponseEntity.ok(new ActionResponse("Stock adjusted successfully"));
    }

    @PostMapping("/transfers")
    public ResponseEntity<ActionResponse> createStockTransfer(
            @Valid @RequestBody CreateStockTransferRequest request) {
        inventoryService.transferStock(
                request.productId(),
                request.fromWarehouseId(),
                request.toWarehouseId(),
                request.quantity(),
                request.unitCost(),
                request.reason(),
                request.notes(),
                request.performedBy());

        return ResponseEntity.ok(new ActionResponse("Stock transferred successfully"));
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
