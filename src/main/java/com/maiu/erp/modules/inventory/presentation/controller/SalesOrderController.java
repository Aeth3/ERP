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
import com.maiu.erp.modules.inventory.application.dto.ConfirmSalesOrderRequest;
import com.maiu.erp.modules.inventory.application.dto.CreateSalesOrderRequest;
import com.maiu.erp.modules.inventory.application.dto.IdResponse;
import com.maiu.erp.modules.inventory.application.dto.SalesOrderDto;
import com.maiu.erp.modules.inventory.application.dto.SalesOrderItemDto;
import com.maiu.erp.modules.inventory.application.dto.ShipSalesOrderRequest;
import com.maiu.erp.modules.inventory.application.service.SalesOrderService;
import com.maiu.erp.modules.inventory.domain.model.SalesOrder;
import com.maiu.erp.modules.inventory.domain.model.SalesOrderItem;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/sales-orders")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    @PostMapping
    public ResponseEntity<IdResponse> createSalesOrder(
            @Valid @RequestBody CreateSalesOrderRequest request) {
        UUID salesOrderId = salesOrderService.createSalesOrder(request);

        return ResponseEntity.status(201).body(new IdResponse(salesOrderId));
    }

    @GetMapping
    public ResponseEntity<List<SalesOrderDto>> getSalesOrders() {
        return ResponseEntity.ok(
                salesOrderService.getSalesOrders().stream()
                        .map(this::toDto)
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesOrderDto> getSalesOrderById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                toDto(salesOrderService.getSalesOrderById(id)));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ActionResponse> confirmSalesOrder(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmSalesOrderRequest request) {
        salesOrderService.confirmSalesOrder(id, request.warehouseId());

        return ResponseEntity.ok(
                new ActionResponse("Sales order confirmed successfully"));
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<ActionResponse> shipSalesOrder(
            @PathVariable UUID id,
            @Valid @RequestBody ShipSalesOrderRequest request) {
        salesOrderService.shipSalesOrder(
                id,
                request.warehouseId(),
                request.performedBy());

        return ResponseEntity.ok(
                new ActionResponse("Sales order shipped successfully"));
    }

    private SalesOrderDto toDto(SalesOrder salesOrder) {
        List<SalesOrderItemDto> items = salesOrderService
                .getSalesOrderItems(salesOrder.getId())
                .stream()
                .map(this::toItemDto)
                .toList();

        return new SalesOrderDto(
                salesOrder.getId(),
                salesOrder.getSoNumber(),
                salesOrder.getCustomerId(),
                salesOrder.getStatus(),
                salesOrder.getOrderDate(),
                salesOrder.getTotalAmount(),
                items);
    }

    private SalesOrderItemDto toItemDto(SalesOrderItem item) {
        return new SalesOrderItemDto(
                item.getId(),
                item.getSalesOrderId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
    }
}
