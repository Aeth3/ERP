package com.maiu.erp.modules.inventory.application.service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.application.dto.CreateSalesOrderItemRequest;
import com.maiu.erp.modules.inventory.application.dto.CreateSalesOrderRequest;
import com.maiu.erp.modules.inventory.application.dto.CreateSalesReturnItemRequest;
import com.maiu.erp.modules.inventory.application.dto.CreateSalesReturnRequest;
import com.maiu.erp.modules.inventory.domain.enums.SalesOrderStatus;
import com.maiu.erp.modules.inventory.domain.model.SalesOrder;
import com.maiu.erp.modules.inventory.domain.model.SalesOrderItem;
import com.maiu.erp.modules.inventory.domain.model.SalesReturn;
import com.maiu.erp.modules.inventory.domain.model.SalesReturnItem;
import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesReturnItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesReturnRepository;
import com.maiu.erp.modules.inventory.domain.repository.StockMovementRepository;
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
    private final SalesReturnRepository salesReturnRepository;
    private final SalesReturnItemRepository salesReturnItemRepository;
    private final StockMovementRepository stockMovementRepository;

    @Autowired
    public SalesOrderService(
            SalesOrderRepository salesOrderRepository,
            SalesOrderItemRepository salesOrderItemRepository,
            InventoryService inventoryService,
            InventoryValidationService inventoryValidationService,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            SalesReturnRepository salesReturnRepository,
            SalesReturnItemRepository salesReturnItemRepository,
            StockMovementRepository stockMovementRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.inventoryService = inventoryService;
        this.inventoryValidationService = inventoryValidationService;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.salesReturnRepository = salesReturnRepository;
        this.salesReturnItemRepository = salesReturnItemRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public SalesOrderService(
            SalesOrderRepository salesOrderRepository,
            SalesOrderItemRepository salesOrderItemRepository,
            InventoryService inventoryService,
            InventoryValidationService inventoryValidationService,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            SalesReturnRepository salesReturnRepository,
            SalesReturnItemRepository salesReturnItemRepository) {
        this(
                salesOrderRepository,
                salesOrderItemRepository,
                inventoryService,
                inventoryValidationService,
                productRepository,
                customerRepository,
                salesReturnRepository,
                salesReturnItemRepository,
                null);
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

        salesOrder.setConfirmedWarehouseId(warehouseId);
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
        validateConfirmedWarehouse(salesOrder, warehouseId);
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderId(salesOrder.getId());
        for (SalesOrderItem item : items) {
            inventoryService.decreaseStock(
                    item.getProductId(),
                    salesOrder.getConfirmedWarehouseId(),
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
            UUID confirmedWarehouseId = salesOrder.getConfirmedWarehouseId();
            if (warehouseId != null && !warehouseId.equals(confirmedWarehouseId)) {
                throw new BadRequestException("Sales Order must be cancelled from the confirmed warehouse");
            }

            List<SalesOrderItem> items = salesOrderItemRepository
                    .findBySalesOrderId(salesOrderId);

            for (SalesOrderItem item : items) {
                inventoryService.releaseReservedStock(
                        item.getProductId(),
                        confirmedWarehouseId,
                        item.getQuantity());
            }

            salesOrder.setConfirmedWarehouseId(null);
        }

        salesOrder.setStatus(SalesOrderStatus.CANCELLED);
        salesOrderRepository.save(salesOrder);
    }

    @Transactional
    public UUID createSalesReturn(UUID salesOrderId, CreateSalesReturnRequest request) {
        SalesOrder salesOrder = getSalesOrderById(salesOrderId);
        if (salesOrder.getStatus() != SalesOrderStatus.SHIPPED
                && salesOrder.getStatus() != SalesOrderStatus.DELIVERED) {
            throw new BadRequestException("Sales order must be shipped or delivered");
        }
        UUID confirmedWarehouseId = resolveConfirmedWarehouseIdForReturn(salesOrder);
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Sales return must have at least one item");
        }

        String performedBy = normalizePerformedBy(request.performedBy());
        List<SalesOrderItem> salesOrderItems = salesOrderItemRepository.findBySalesOrderId(salesOrderId);
        if (salesOrderItems.isEmpty()) {
            throw new BadRequestException("Sales order has no items");
        }

        SalesReturn salesReturn = new SalesReturn();
        salesReturn.setReturnNumber(generateSalesReturnNumber());
        salesReturn.setSalesOrderId(salesOrderId);
        salesReturn.setCustomerId(salesOrder.getCustomerId());
        salesReturn.setWarehouseId(confirmedWarehouseId);
        salesReturn.setRemarks(trimToNull(request.remarks()));
        salesReturn.setPerformedBy(performedBy);
        salesReturn.setReturnedAt(java.time.Instant.now());
        SalesReturn savedReturn = salesReturnRepository.save(salesReturn);

        for (CreateSalesReturnItemRequest itemRequest : request.items()) {
            validateSalesReturnItem(itemRequest, salesOrderItems, salesOrderId);

            SalesOrderItem sourceItem = salesOrderItems.stream()
                    .filter(item -> itemRequest.productId().equals(item.getProductId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Sales return item product was not on the sales order"));

            SalesReturnItem item = new SalesReturnItem();
            item.setSalesReturnId(savedReturn.getId());
            item.setProductId(itemRequest.productId());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(sourceItem.getUnitPrice());
            item.setLineTotal(itemRequest.quantity().multiply(sourceItem.getUnitPrice()));
            salesReturnItemRepository.save(item);

            inventoryService.receiveSalesReturn(
                    item.getProductId(),
                    savedReturn.getWarehouseId(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    savedReturn.getId(),
                    savedReturn.getRemarks(),
                    performedBy);
        }

        return savedReturn.getId();
    }

    public List<SalesReturn> getSalesReturns() {
        return salesReturnRepository.findAll();
    }

    public SalesReturn getSalesReturnById(UUID returnId) {
        return salesReturnRepository.findById(returnId)
                .orElseThrow(() -> new NotFoundException("Sales return not found"));
    }

    public List<SalesReturnItem> getSalesReturnItems(UUID returnId) {
        return salesReturnItemRepository.findBySalesReturnId(returnId);
    }

    private void validateConfirmedWarehouse(
            SalesOrder salesOrder,
            UUID warehouseId) {
        UUID confirmedWarehouseId = salesOrder.getConfirmedWarehouseId();
        if (confirmedWarehouseId == null) {
            throw new BadRequestException("Sales Order has no confirmed warehouse");
        }
        if (!confirmedWarehouseId.equals(warehouseId)) {
            throw new BadRequestException("Sales Order must be shipped from the confirmed warehouse");
        }
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

    private void validateSalesReturnItem(
            CreateSalesReturnItemRequest itemRequest,
            List<SalesOrderItem> salesOrderItems,
            UUID salesOrderId) {
        if (itemRequest.productId() == null) {
            throw new BadRequestException("Product ID is required");
        }
        if (itemRequest.quantity() == null || itemRequest.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Return quantity must be greater than zero");
        }

        SalesOrderItem sourceItem = salesOrderItems.stream()
                .filter(item -> itemRequest.productId().equals(item.getProductId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Sales return item product was not on the sales order"));

        BigDecimal alreadyReturnedQuantity = getReturnedQuantityForProduct(salesOrderId, itemRequest.productId());
        BigDecimal newReturnedQuantity = alreadyReturnedQuantity.add(itemRequest.quantity());
        if (newReturnedQuantity.compareTo(sourceItem.getQuantity()) > 0) {
            throw new BadRequestException("Sales return quantity exceeds shipped quantity for product");
        }
    }

    private BigDecimal getReturnedQuantityForProduct(UUID salesOrderId, UUID productId) {
        BigDecimal total = BigDecimal.ZERO;
        for (SalesReturn salesReturn : salesReturnRepository.findBySalesOrderId(salesOrderId)) {
            for (SalesReturnItem item : salesReturnItemRepository.findBySalesReturnId(salesReturn.getId())) {
                if (productId.equals(item.getProductId())) {
                    total = total.add(item.getQuantity());
                }
            }
        }
        return total;
    }

    private UUID resolveConfirmedWarehouseIdForReturn(SalesOrder salesOrder) {
        if (salesOrder.getConfirmedWarehouseId() != null) {
            return salesOrder.getConfirmedWarehouseId();
        }
        if (stockMovementRepository == null) {
            throw new BadRequestException("Sales order has no confirmed warehouse");
        }

        Set<UUID> candidateWarehouses = new LinkedHashSet<>(
                stockMovementRepository.findByReferenceId(salesOrder.getId()).stream()
                        .filter(movement -> "SALES_ORDER".equals(movement.getReferenceType()))
                        .map(movement -> movement.getWarehouseId())
                        .toList());

        if (candidateWarehouses.isEmpty()) {
            throw new BadRequestException("Sales order has no confirmed warehouse");
        }
        if (candidateWarehouses.size() > 1) {
            throw new BadRequestException("Sales order has multiple confirmed warehouses");
        }

        UUID resolvedWarehouseId = candidateWarehouses.iterator().next();
        salesOrder.setConfirmedWarehouseId(resolvedWarehouseId);
        salesOrderRepository.save(salesOrder);
        return resolvedWarehouseId;
    }

    private String normalizePerformedBy(String performedBy) {
        if (performedBy == null || performedBy.isBlank()) {
            throw new BadRequestException("performedBy is required");
        }
        return performedBy.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private String generateSalesReturnNumber() {
        String returnNumber;
        do {
            returnNumber = "SR-" + UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .toUpperCase();
        } while (salesReturnRepository.findByReturnNumber(returnNumber).isPresent());

        return returnNumber;
    }
}
