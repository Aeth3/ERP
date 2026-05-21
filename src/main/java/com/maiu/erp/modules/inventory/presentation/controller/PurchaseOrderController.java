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
import com.maiu.erp.modules.inventory.application.dto.IdResponse;
import com.maiu.erp.modules.inventory.application.dto.PurchaseOrderDto;
import com.maiu.erp.modules.inventory.application.dto.PurchaseOrderItemDto;
import com.maiu.erp.modules.inventory.application.dto.ReceivePurchaseOrderRequest;
import com.maiu.erp.modules.inventory.application.service.PurchaseOrderService;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrderItem;

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
                purchaseOrder.getStatus(),
                purchaseOrder.getOrderDate(),
                purchaseOrder.getExpectedDate(),
                purchaseOrder.getTotalAmount(),
                purchaseOrder.getCreatedAt(),
                items);
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
}
