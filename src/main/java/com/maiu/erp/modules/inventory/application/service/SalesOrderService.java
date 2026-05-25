package com.maiu.erp.modules.inventory.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.application.dto.CreateSalesOrderItemRequest;
import com.maiu.erp.modules.inventory.application.dto.CreateSalesOrderRequest;
import com.maiu.erp.modules.inventory.domain.enums.SalesOrderStatus;
import com.maiu.erp.modules.inventory.domain.model.SalesOrder;
import com.maiu.erp.modules.inventory.domain.model.SalesOrderItem;
import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;

@Service
public class SalesOrderService {
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final InventoryService inventoryService;
    private final InventoryValidationService inventoryValidationService;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public SalesOrderService(
            SalesOrderRepository salesOrderRepository,
            SalesOrderItemRepository salesOrderItemRepository,
            InventoryService inventoryService,
            InventoryValidationService inventoryValidationService,
            ProductRepository productRepository,
            CustomerRepository customerRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.inventoryService = inventoryService;
        this.inventoryValidationService = inventoryValidationService;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
    }

    public List<SalesOrder> getSalesOrders() {
        return salesOrderRepository.findAll();
    }

    public SalesOrder getSalesOrderById(
            UUID salesOrderId) {
        return salesOrderRepository.findById(salesOrderId)
                .orElseThrow(() -> new NotFoundException("Sales Order not found"));
    }

    public List<SalesOrderItem> getSalesOrderItems(
            UUID salesOrderId) {
        return salesOrderItemRepository.findBySalesOrderId(salesOrderId);
    }

    @Transactional
    public UUID createSalesOrder(
            CreateSalesOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Sales order must have at least one item");
        }
        if (request.customerId() == null) {
            throw new BadRequestException("Customer ID is required");
        }
        if (request.orderDate() == null) {
            throw new BadRequestException("Order date is required");
        }
        customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setSoNumber(generateSoNumber());
        salesOrder.setCustomerId(request.customerId());
        salesOrder.setStatus(SalesOrderStatus.DRAFT);
        salesOrder.setOrderDate(request.orderDate());
        salesOrder.setTotalAmount(calculateTotalAmount(request.items()));

        SalesOrder savedSalesOrder = salesOrderRepository.save(salesOrder);

        for (CreateSalesOrderItemRequest itemRequest : request.items()) {
            validateSalesOrderItem(itemRequest);

            SalesOrderItem item = new SalesOrderItem();
            item.setSalesOrderId(savedSalesOrder.getId());
            item.setProductId(itemRequest.productId());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(itemRequest.unitPrice());
            item.setLineTotal(itemRequest.quantity().multiply(itemRequest.unitPrice()));
            salesOrderItemRepository.save(item);
        }

        return savedSalesOrder.getId();
    }

    @Transactional
    public void confirmSalesOrder(
            UUID salesOrderId,
            UUID warehouseId) {
        inventoryValidationService.validateWarehouseExists(warehouseId);

        SalesOrder salesOrder = salesOrderRepository.findById(salesOrderId)
                .orElseThrow(() -> new NotFoundException("Sales Order not found"));

        if (salesOrder.getStatus() != SalesOrderStatus.DRAFT) {
            throw new BadRequestException("Only draft sales orders can be confirmed");
        }

        List<SalesOrderItem> items = salesOrderItemRepository
                .findBySalesOrderId(salesOrderId);

        if (items.isEmpty()) {
            throw new BadRequestException("Sales Order has no items");
        }

        for (SalesOrderItem item : items) {
            inventoryValidationService.validateProductActive(
                    item.getProductId());
            inventoryValidationService.validateAvailableStock(
                    item.getProductId(),
                    warehouseId,
                    item.getQuantity());
            inventoryService.reserveStock(
                    item.getProductId(),
                    warehouseId,
                    item.getQuantity());
        }

        salesOrder.setStatus(SalesOrderStatus.CONFIRMED);
        salesOrderRepository.save(salesOrder);
    }

    @Transactional
    public void shipSalesOrder(
            UUID salesOrderId,
            UUID warehouseId,
            String performedBy) {
        inventoryValidationService.validateWarehouseExists(warehouseId);
        SalesOrder salesOrder = salesOrderRepository.findById(salesOrderId)
                .orElseThrow(() -> new NotFoundException("Sales Order not found"));

        if (salesOrder.getStatus() != SalesOrderStatus.CONFIRMED) {
            throw new BadRequestException("Sales Order must be confirmed");
        }
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderId(salesOrder.getId());
        for (SalesOrderItem item : items) {
            inventoryService.decreaseStock(
                    item.getProductId(),
                    warehouseId,
                    item.getQuantity(),
                    item.getUnitPrice(),
                    "SALES_ORDER",
                    salesOrder.getId(),
                    performedBy);
        }
        salesOrder.setStatus(SalesOrderStatus.SHIPPED);
        salesOrderRepository.save(salesOrder);
    }

    @Transactional
    public void cancelSalesOrder(
            UUID salesOrderId,
            UUID warehouseId) {
        SalesOrder salesOrder = salesOrderRepository.findById(salesOrderId)
                .orElseThrow(() -> new NotFoundException("Sales Order not found"));

        if (salesOrder.getStatus() == SalesOrderStatus.SHIPPED
                || salesOrder.getStatus() == SalesOrderStatus.DELIVERED) {
            throw new BadRequestException("Shipped or delivered sales orders cannot be cancelled");
        }

        if (salesOrder.getStatus() == SalesOrderStatus.CANCELLED) {
            throw new BadRequestException("Sales Order is already cancelled");
        }

        if (salesOrder.getStatus() == SalesOrderStatus.CONFIRMED) {
            inventoryValidationService.validateWarehouseExists(warehouseId);

            List<SalesOrderItem> items = salesOrderItemRepository
                    .findBySalesOrderId(salesOrderId);

            for (SalesOrderItem item : items) {
                inventoryService.releaseReservedStock(
                        item.getProductId(),
                        warehouseId,
                        item.getQuantity());
            }
        }

        salesOrder.setStatus(SalesOrderStatus.CANCELLED);
        salesOrderRepository.save(salesOrder);
    }

    private BigDecimal calculateTotalAmount(
            List<CreateSalesOrderItemRequest> items) {
        return items.stream()
                .peek(this::validateSalesOrderItem)
                .map(item -> item.quantity().multiply(item.unitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateSalesOrderItem(
            CreateSalesOrderItemRequest itemRequest) {
        if (itemRequest.productId() == null) {
            throw new BadRequestException("Product ID is required");
        }
        if (itemRequest.quantity() == null
                || itemRequest.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Item quantity must be greater than zero");
        }
        if (itemRequest.unitPrice() == null
                || itemRequest.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Item unit price must be zero or greater");
        }

        productRepository.findById(itemRequest.productId())
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private String generateSoNumber() {
        String soNumber;
        do {
            soNumber = "SO-" + UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .toUpperCase();
        } while (salesOrderRepository.findBySoNumber(soNumber).isPresent());

        return soNumber;
    }
}
