package com.maiu.erp.modules.inventory.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.inventory.application.dto.ActionResponse;
import com.maiu.erp.modules.inventory.application.dto.CreatePurchaseOrderRequest;
import com.maiu.erp.modules.inventory.application.dto.CreatePurchaseReturnRequest;
import com.maiu.erp.modules.inventory.application.dto.IdResponse;
import com.maiu.erp.modules.inventory.application.dto.PurchaseOrderDto;
import com.maiu.erp.modules.inventory.application.dto.PurchaseOrderItemDto;
import com.maiu.erp.modules.inventory.application.dto.PurchaseReturnDto;
import com.maiu.erp.modules.inventory.application.dto.PurchaseReturnItemDto;
import com.maiu.erp.modules.inventory.application.dto.ReceivePurchaseOrderRequest;
import com.maiu.erp.modules.inventory.application.service.PurchaseOrderService;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrderItem;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturn;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturnItem;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    public ResponseEntity<IdResponse> createPurchaseOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request) {
        UUID purchaseOrderId = purchaseOrderService.createPurchaseOrder(request);

        return ResponseEntity.status(201).body(new IdResponse(purchaseOrderId));
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrderDto>> getPurchaseOrders() {
        return ResponseEntity.ok(
                purchaseOrderService.getPurchaseOrders().stream()
                        .map(this::toDto)
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderDto> getPurchaseOrderById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                toDto(purchaseOrderService.getPurchaseOrderById(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ActionResponse> approvePurchaseOrder(
            @PathVariable UUID id) {
        purchaseOrderService.approvePurchaseOrder(id);

        return ResponseEntity.ok(
                new ActionResponse("Purchase order approved successfully"));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<ActionResponse> receivePurchaseOrder(
            @PathVariable UUID id,
            @Valid @RequestBody ReceivePurchaseOrderRequest request) {
        purchaseOrderService.receivePurchaseOrder(
                id,
                request.warehouseId(),
                request.performedBy());

        return ResponseEntity.ok(
                new ActionResponse("Purchase order received successfully"));
    }

    @PostMapping("/{id}/returns")
    public ResponseEntity<IdResponse> createPurchaseReturn(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePurchaseReturnRequest request) {
        return ResponseEntity.status(201).body(new IdResponse(purchaseOrderService.createPurchaseReturn(id, request)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ActionResponse> cancelPurchaseOrder(
            @PathVariable UUID id) {
        purchaseOrderService.cancelPurchaseOrder(id);

        return ResponseEntity.ok(
                new ActionResponse("Purchase order cancelled successfully"));
    }

    @GetMapping("/returns")
    public ResponseEntity<List<PurchaseReturnDto>> getPurchaseReturns() {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseReturns().stream().map(this::toReturnDto).toList());
    }

    @GetMapping("/returns/{id}")
    public ResponseEntity<PurchaseReturnDto> getPurchaseReturnById(@PathVariable UUID id) {
        return ResponseEntity.ok(toReturnDto(purchaseOrderService.getPurchaseReturnById(id)));
    }

    private PurchaseOrderDto toDto(PurchaseOrder purchaseOrder) {
        List<PurchaseOrderItemDto> items = purchaseOrderService
                .getPurchaseOrderItems(purchaseOrder.getId())
                .stream()
                .map(this::toItemDto)
                .toList();

        return new PurchaseOrderDto(
                purchaseOrder.getId(),
                purchaseOrder.getPoNumber(),
                purchaseOrder.getSupplierId(),
                purchaseOrder.getProjectId(),
                purchaseOrder.getStatus(),
                purchaseOrder.getOrderDate(),
                purchaseOrder.getExpectedDate(),
                purchaseOrder.getTotalAmount(),
                purchaseOrder.getCreatedAt(),
                items);
    }

    private PurchaseReturnDto toReturnDto(PurchaseReturn purchaseReturn) {
        return new PurchaseReturnDto(
                purchaseReturn.getId(),
                purchaseReturn.getReturnNumber(),
                purchaseReturn.getPurchaseOrderId(),
                purchaseReturn.getSupplierId(),
                purchaseReturn.getWarehouseId(),
                purchaseReturn.getRemarks(),
                purchaseReturn.getPerformedBy(),
                purchaseReturn.getReturnedAt(),
                purchaseOrderService.getPurchaseReturnItems(purchaseReturn.getId()).stream().map(this::toReturnItemDto).toList());
    }

    private PurchaseOrderItemDto toItemDto(PurchaseOrderItem item) {
        return new PurchaseOrderItemDto(
                item.getId(),
                item.getPurchaseOrderId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getLineTotal());
    }

    private PurchaseReturnItemDto toReturnItemDto(PurchaseReturnItem item) {
        return new PurchaseReturnItemDto(
                item.getId(),
                item.getPurchaseReturnId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getLineTotal());
    }
}
