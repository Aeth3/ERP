package com.maiu.erp.modules.inventory.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.inventory.domain.enums.MovementType;
import com.maiu.erp.modules.inventory.domain.enums.SalesOrderStatus;
import com.maiu.erp.modules.inventory.domain.model.Customer;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.SalesOrder;
import com.maiu.erp.modules.inventory.domain.model.SalesOrderItem;
import com.maiu.erp.modules.inventory.domain.model.SalesReturn;
import com.maiu.erp.modules.inventory.domain.model.SalesReturnItem;
import com.maiu.erp.modules.inventory.domain.model.StockMovement;
import com.maiu.erp.modules.inventory.domain.model.Warehouse;
import com.maiu.erp.modules.inventory.domain.repository.InventoryStockRepository;
import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesReturnItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesReturnRepository;
import com.maiu.erp.modules.inventory.domain.repository.StockMovementRepository;
import com.maiu.erp.modules.inventory.domain.repository.WarehouseRepository;

class SalesOrderServiceTest {

    @Test
    void confirmSalesOrderReservesStockAndMarksOrderConfirmed() {
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemorySalesOrderRepository salesOrderRepository = new InMemorySalesOrderRepository();
        InMemorySalesOrderItemRepository salesOrderItemRepository = new InMemorySalesOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();

        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID salesOrderId = UUID.randomUUID();

        productRepository.save(activeProduct(productId));
        warehouseRepository.save(activeWarehouse(warehouseId));
        inventoryStockRepository.save(stock(productId, warehouseId, "10", "2"));
        salesOrderRepository.save(salesOrder(salesOrderId, SalesOrderStatus.DRAFT));
        salesOrderItemRepository.save(salesOrderItem(salesOrderId, productId, "4", "12.50"));

        InventoryValidationService validationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                validationService);
        SalesOrderService salesOrderService = new SalesOrderService(
                salesOrderRepository,
                salesOrderItemRepository,
                inventoryService,
                validationService,
                productRepository,
                customerRepository,
                new InMemorySalesReturnRepository(),
                new InMemorySalesReturnItemRepository());

        salesOrderService.confirmSalesOrder(salesOrderId, warehouseId);

        InventoryStock updatedStock = inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow();
        SalesOrder updatedOrder = salesOrderRepository.findById(salesOrderId)
                .orElseThrow();

        assertEquals(new BigDecimal("10"), updatedStock.getQuantityOnHand());
        assertEquals(new BigDecimal("6"), updatedStock.getReservedQuantity());
        assertEquals(SalesOrderStatus.CONFIRMED, updatedOrder.getStatus());
        assertEquals(warehouseId, updatedOrder.getConfirmedWarehouseId());
    }

    @Test
    void confirmSalesOrderFailsWhenAvailableStockIsInsufficient() {
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemorySalesOrderRepository salesOrderRepository = new InMemorySalesOrderRepository();
        InMemorySalesOrderItemRepository salesOrderItemRepository = new InMemorySalesOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();

        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID salesOrderId = UUID.randomUUID();

        productRepository.save(activeProduct(productId));
        warehouseRepository.save(activeWarehouse(warehouseId));
        inventoryStockRepository.save(stock(productId, warehouseId, "10", "8"));
        salesOrderRepository.save(salesOrder(salesOrderId, SalesOrderStatus.DRAFT));
        salesOrderItemRepository.save(salesOrderItem(salesOrderId, productId, "3", "12.50"));

        InventoryValidationService validationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                validationService);
        SalesOrderService salesOrderService = new SalesOrderService(
                salesOrderRepository,
                salesOrderItemRepository,
                inventoryService,
                validationService,
                productRepository,
                customerRepository,
                new InMemorySalesReturnRepository(),
                new InMemorySalesReturnItemRepository());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> salesOrderService.confirmSalesOrder(salesOrderId, warehouseId));

        InventoryStock unchangedStock = inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow();

        assertEquals("Insufficient available stock", exception.getMessage());
        assertEquals(new BigDecimal("8"), unchangedStock.getReservedQuantity());
        assertEquals(
                SalesOrderStatus.DRAFT,
                salesOrderRepository.findById(salesOrderId).orElseThrow().getStatus());
    }

    @Test
    void shipSalesOrderConsumesReservedStockAndRecordsMovement() {
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemorySalesOrderRepository salesOrderRepository = new InMemorySalesOrderRepository();
        InMemorySalesOrderItemRepository salesOrderItemRepository = new InMemorySalesOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();

        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID salesOrderId = UUID.randomUUID();
        String performedBy = "Alex Reyes";

        productRepository.save(activeProduct(productId));
        warehouseRepository.save(activeWarehouse(warehouseId));
        inventoryStockRepository.save(stock(productId, warehouseId, "10", "4"));
        SalesOrder confirmedOrder = salesOrder(salesOrderId, SalesOrderStatus.CONFIRMED);
        confirmedOrder.setConfirmedWarehouseId(warehouseId);
        salesOrderRepository.save(confirmedOrder);
        salesOrderItemRepository.save(salesOrderItem(salesOrderId, productId, "4", "15.00"));

        InventoryValidationService validationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                validationService);
        SalesOrderService salesOrderService = new SalesOrderService(
                salesOrderRepository,
                salesOrderItemRepository,
                inventoryService,
                validationService,
                productRepository,
                customerRepository,
                new InMemorySalesReturnRepository(),
                new InMemorySalesReturnItemRepository());

        salesOrderService.shipSalesOrder(salesOrderId, warehouseId, performedBy);

        InventoryStock updatedStock = inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow();
        SalesOrder updatedOrder = salesOrderRepository.findById(salesOrderId)
                .orElseThrow();
        StockMovement movement = stockMovementRepository.movements().getFirst();

        assertEquals(new BigDecimal("6"), updatedStock.getQuantityOnHand());
        assertEquals(BigDecimal.ZERO, updatedStock.getReservedQuantity());
        assertEquals(SalesOrderStatus.SHIPPED, updatedOrder.getStatus());
        assertEquals(MovementType.SALES_OUT, movement.getMovementType());
        assertEquals(new BigDecimal("15.00"), movement.getUnitCost());
        assertEquals(salesOrderId, movement.getReferenceId());
        assertEquals(performedBy, movement.getPerformedBy());
    }

    @Test
    void shipSalesOrderFailsWhenWarehouseDoesNotMatchConfirmedWarehouse() {
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemorySalesOrderRepository salesOrderRepository = new InMemorySalesOrderRepository();
        InMemorySalesOrderItemRepository salesOrderItemRepository = new InMemorySalesOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();

        UUID productId = UUID.randomUUID();
        UUID confirmedWarehouseId = UUID.randomUUID();
        UUID wrongWarehouseId = UUID.randomUUID();
        UUID salesOrderId = UUID.randomUUID();

        productRepository.save(activeProduct(productId));
        warehouseRepository.save(activeWarehouse(confirmedWarehouseId));
        warehouseRepository.save(activeWarehouse(wrongWarehouseId));
        inventoryStockRepository.save(stock(productId, confirmedWarehouseId, "10", "4"));
        SalesOrder confirmedOrder = salesOrder(salesOrderId, SalesOrderStatus.CONFIRMED);
        confirmedOrder.setConfirmedWarehouseId(confirmedWarehouseId);
        salesOrderRepository.save(confirmedOrder);
        salesOrderItemRepository.save(salesOrderItem(salesOrderId, productId, "4", "15.00"));

        InventoryValidationService validationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                validationService);
        SalesOrderService salesOrderService = new SalesOrderService(
                salesOrderRepository,
                salesOrderItemRepository,
                inventoryService,
                validationService,
                productRepository,
                customerRepository,
                new InMemorySalesReturnRepository(),
                new InMemorySalesReturnItemRepository());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> salesOrderService.shipSalesOrder(salesOrderId, wrongWarehouseId, "Alex Reyes"));

        assertEquals("Sales Order must be shipped from the confirmed warehouse", exception.getMessage());
        assertEquals(new BigDecimal("10"), inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, confirmedWarehouseId)
                .orElseThrow()
                .getQuantityOnHand());
    }

    @Test
    void cancelDraftSalesOrderMarksOrderCancelledWithoutTouchingStock() {
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemorySalesOrderRepository salesOrderRepository = new InMemorySalesOrderRepository();
        InMemorySalesOrderItemRepository salesOrderItemRepository = new InMemorySalesOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();

        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID salesOrderId = UUID.randomUUID();

        productRepository.save(activeProduct(productId));
        warehouseRepository.save(activeWarehouse(warehouseId));
        inventoryStockRepository.save(stock(productId, warehouseId, "10", "2"));
        salesOrderRepository.save(salesOrder(salesOrderId, SalesOrderStatus.DRAFT));
        salesOrderItemRepository.save(salesOrderItem(salesOrderId, productId, "4", "12.50"));

        InventoryValidationService validationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                validationService);
        SalesOrderService salesOrderService = new SalesOrderService(
                salesOrderRepository,
                salesOrderItemRepository,
                inventoryService,
                validationService,
                productRepository,
                customerRepository,
                new InMemorySalesReturnRepository(),
                new InMemorySalesReturnItemRepository());

        salesOrderService.cancelSalesOrder(salesOrderId, null);

        InventoryStock unchangedStock = inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow();
        SalesOrder updatedOrder = salesOrderRepository.findById(salesOrderId)
                .orElseThrow();

        assertEquals(new BigDecimal("10"), unchangedStock.getQuantityOnHand());
        assertEquals(new BigDecimal("2"), unchangedStock.getReservedQuantity());
        assertEquals(SalesOrderStatus.CANCELLED, updatedOrder.getStatus());
    }

    @Test
    void cancelConfirmedSalesOrderReleasesReservedStockAndMarksOrderCancelled() {
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemorySalesOrderRepository salesOrderRepository = new InMemorySalesOrderRepository();
        InMemorySalesOrderItemRepository salesOrderItemRepository = new InMemorySalesOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();

        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID salesOrderId = UUID.randomUUID();

        productRepository.save(activeProduct(productId));
        warehouseRepository.save(activeWarehouse(warehouseId));
        inventoryStockRepository.save(stock(productId, warehouseId, "10", "6"));
        SalesOrder confirmedOrder = salesOrder(salesOrderId, SalesOrderStatus.CONFIRMED);
        confirmedOrder.setConfirmedWarehouseId(warehouseId);
        salesOrderRepository.save(confirmedOrder);
        salesOrderItemRepository.save(salesOrderItem(salesOrderId, productId, "4", "12.50"));

        InventoryValidationService validationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                validationService);
        SalesOrderService salesOrderService = new SalesOrderService(
                salesOrderRepository,
                salesOrderItemRepository,
                inventoryService,
                validationService,
                productRepository,
                customerRepository,
                new InMemorySalesReturnRepository(),
                new InMemorySalesReturnItemRepository());

        salesOrderService.cancelSalesOrder(salesOrderId, warehouseId);

        InventoryStock updatedStock = inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow();
        SalesOrder updatedOrder = salesOrderRepository.findById(salesOrderId)
                .orElseThrow();

        assertEquals(new BigDecimal("10"), updatedStock.getQuantityOnHand());
        assertEquals(new BigDecimal("2"), updatedStock.getReservedQuantity());
        assertEquals(SalesOrderStatus.CANCELLED, updatedOrder.getStatus());
    }

    @Test
    void cancelConfirmedSalesOrderFailsWhenWarehouseDoesNotMatchConfirmedWarehouse() {
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemorySalesOrderRepository salesOrderRepository = new InMemorySalesOrderRepository();
        InMemorySalesOrderItemRepository salesOrderItemRepository = new InMemorySalesOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();

        UUID productId = UUID.randomUUID();
        UUID confirmedWarehouseId = UUID.randomUUID();
        UUID wrongWarehouseId = UUID.randomUUID();
        UUID salesOrderId = UUID.randomUUID();

        productRepository.save(activeProduct(productId));
        warehouseRepository.save(activeWarehouse(confirmedWarehouseId));
        warehouseRepository.save(activeWarehouse(wrongWarehouseId));
        inventoryStockRepository.save(stock(productId, confirmedWarehouseId, "10", "6"));
        SalesOrder confirmedOrder = salesOrder(salesOrderId, SalesOrderStatus.CONFIRMED);
        confirmedOrder.setConfirmedWarehouseId(confirmedWarehouseId);
        salesOrderRepository.save(confirmedOrder);
        salesOrderItemRepository.save(salesOrderItem(salesOrderId, productId, "4", "12.50"));

        InventoryValidationService validationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                validationService);
        SalesOrderService salesOrderService = new SalesOrderService(
                salesOrderRepository,
                salesOrderItemRepository,
                inventoryService,
                validationService,
                productRepository,
                customerRepository,
                new InMemorySalesReturnRepository(),
                new InMemorySalesReturnItemRepository());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> salesOrderService.cancelSalesOrder(salesOrderId, wrongWarehouseId));

        assertEquals("Sales Order must be cancelled from the confirmed warehouse", exception.getMessage());
        assertEquals(new BigDecimal("6"), inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, confirmedWarehouseId)
                .orElseThrow()
                .getReservedQuantity());
    }

    @Test
    void cancelShippedSalesOrderFails() {
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemorySalesOrderRepository salesOrderRepository = new InMemorySalesOrderRepository();
        InMemorySalesOrderItemRepository salesOrderItemRepository = new InMemorySalesOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();

        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID salesOrderId = UUID.randomUUID();

        productRepository.save(activeProduct(productId));
        warehouseRepository.save(activeWarehouse(warehouseId));
        inventoryStockRepository.save(stock(productId, warehouseId, "10", "0"));
        salesOrderRepository.save(salesOrder(salesOrderId, SalesOrderStatus.SHIPPED));
        salesOrderItemRepository.save(salesOrderItem(salesOrderId, productId, "4", "12.50"));

        InventoryValidationService validationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                validationService);
        SalesOrderService salesOrderService = new SalesOrderService(
                salesOrderRepository,
                salesOrderItemRepository,
                inventoryService,
                validationService,
                productRepository,
                customerRepository,
                new InMemorySalesReturnRepository(),
                new InMemorySalesReturnItemRepository());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> salesOrderService.cancelSalesOrder(salesOrderId, warehouseId));

        assertEquals("Shipped or delivered sales orders cannot be cancelled", exception.getMessage());
        assertEquals(
                SalesOrderStatus.SHIPPED,
                salesOrderRepository.findById(salesOrderId).orElseThrow().getStatus());
    }

    @Test
    void createSalesReturnRestoresStockAndPersistsReturn() {
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemorySalesOrderRepository salesOrderRepository = new InMemorySalesOrderRepository();
        InMemorySalesOrderItemRepository salesOrderItemRepository = new InMemorySalesOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();
        InMemorySalesReturnRepository salesReturnRepository = new InMemorySalesReturnRepository();
        InMemorySalesReturnItemRepository salesReturnItemRepository = new InMemorySalesReturnItemRepository();

        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID salesOrderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        productRepository.save(activeProduct(productId));
        warehouseRepository.save(activeWarehouse(warehouseId));
        customerRepository.save(activeCustomer(customerId));
        inventoryStockRepository.save(stock(productId, warehouseId, "6", "0"));
        SalesOrder shippedOrder = salesOrder(salesOrderId, SalesOrderStatus.SHIPPED);
        shippedOrder.setCustomerId(customerId);
        shippedOrder.setConfirmedWarehouseId(warehouseId);
        salesOrderRepository.save(shippedOrder);
        salesOrderItemRepository.save(salesOrderItem(salesOrderId, productId, "4", "15.00"));

        InventoryValidationService validationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                validationService);
        SalesOrderService salesOrderService = new SalesOrderService(
                salesOrderRepository,
                salesOrderItemRepository,
                inventoryService,
                validationService,
                productRepository,
                customerRepository,
                salesReturnRepository,
                salesReturnItemRepository);

        UUID salesReturnId = salesOrderService.createSalesReturn(
                salesOrderId,
                new com.maiu.erp.modules.inventory.application.dto.CreateSalesReturnRequest(
                        "Customer returned damaged item",
                        "warehouse lead",
                        List.of(new com.maiu.erp.modules.inventory.application.dto.CreateSalesReturnItemRequest(
                                productId,
                                new BigDecimal("2.00")))));

        SalesReturn salesReturn = salesReturnRepository.findById(salesReturnId).orElseThrow();
        assertEquals(warehouseId, salesReturn.getWarehouseId());
        assertEquals(new BigDecimal("8.00"), inventoryStockRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow()
                .getQuantityOnHand());
        assertEquals(1, salesReturnItemRepository.findBySalesReturnId(salesReturnId).size());
    }

    @Test
    void createSalesReturnFailsWhenQuantityExceedsShippedQuantity() {
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemorySalesOrderRepository salesOrderRepository = new InMemorySalesOrderRepository();
        InMemorySalesOrderItemRepository salesOrderItemRepository = new InMemorySalesOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();
        InMemorySalesReturnRepository salesReturnRepository = new InMemorySalesReturnRepository();
        InMemorySalesReturnItemRepository salesReturnItemRepository = new InMemorySalesReturnItemRepository();

        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID salesOrderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        productRepository.save(activeProduct(productId));
        warehouseRepository.save(activeWarehouse(warehouseId));
        customerRepository.save(activeCustomer(customerId));
        inventoryStockRepository.save(stock(productId, warehouseId, "6", "0"));
        SalesOrder shippedOrder = salesOrder(salesOrderId, SalesOrderStatus.SHIPPED);
        shippedOrder.setCustomerId(customerId);
        shippedOrder.setConfirmedWarehouseId(warehouseId);
        salesOrderRepository.save(shippedOrder);
        salesOrderItemRepository.save(salesOrderItem(salesOrderId, productId, "4", "15.00"));

        InventoryValidationService validationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                validationService);
        SalesOrderService salesOrderService = new SalesOrderService(
                salesOrderRepository,
                salesOrderItemRepository,
                inventoryService,
                validationService,
                productRepository,
                customerRepository,
                salesReturnRepository,
                salesReturnItemRepository);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> salesOrderService.createSalesReturn(
                        salesOrderId,
                        new com.maiu.erp.modules.inventory.application.dto.CreateSalesReturnRequest(
                                "Over return",
                                "warehouse lead",
                                List.of(new com.maiu.erp.modules.inventory.application.dto.CreateSalesReturnItemRequest(
                                        productId,
                                        new BigDecimal("5.00"))))));

        assertEquals("Sales return quantity exceeds shipped quantity for product", exception.getMessage());
    }

    @Test
    void createSalesReturnBackfillsConfirmedWarehouseFromSalesMovements() {
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemorySalesOrderRepository salesOrderRepository = new InMemorySalesOrderRepository();
        InMemorySalesOrderItemRepository salesOrderItemRepository = new InMemorySalesOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();
        InMemorySalesReturnRepository salesReturnRepository = new InMemorySalesReturnRepository();
        InMemorySalesReturnItemRepository salesReturnItemRepository = new InMemorySalesReturnItemRepository();

        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID salesOrderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        productRepository.save(activeProduct(productId));
        warehouseRepository.save(activeWarehouse(warehouseId));
        customerRepository.save(activeCustomer(customerId));
        inventoryStockRepository.save(stock(productId, warehouseId, "6", "0"));

        SalesOrder shippedOrder = salesOrder(salesOrderId, SalesOrderStatus.SHIPPED);
        shippedOrder.setCustomerId(customerId);
        salesOrderRepository.save(shippedOrder);
        salesOrderItemRepository.save(salesOrderItem(salesOrderId, productId, "4", "15.00"));

        StockMovement shipMovement = new StockMovement();
        shipMovement.setProductId(productId);
        shipMovement.setWarehouseId(warehouseId);
        shipMovement.setMovementType(MovementType.SALES_OUT);
        shipMovement.setQuantity(new BigDecimal("4.00"));
        shipMovement.setUnitCost(new BigDecimal("15.00"));
        shipMovement.setReferenceType("SALES_ORDER");
        shipMovement.setReferenceId(salesOrderId);
        shipMovement.setMovementDate(Instant.now());
        shipMovement.setPerformedBy("warehouse lead");
        stockMovementRepository.save(shipMovement);

        InventoryValidationService validationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                validationService);
        SalesOrderService salesOrderService = new SalesOrderService(
                salesOrderRepository,
                salesOrderItemRepository,
                inventoryService,
                validationService,
                productRepository,
                customerRepository,
                salesReturnRepository,
                salesReturnItemRepository,
                stockMovementRepository);

        UUID salesReturnId = salesOrderService.createSalesReturn(
                salesOrderId,
                new com.maiu.erp.modules.inventory.application.dto.CreateSalesReturnRequest(
                        "Legacy SO return",
                        "warehouse lead",
                        List.of(new com.maiu.erp.modules.inventory.application.dto.CreateSalesReturnItemRequest(
                                productId,
                                new BigDecimal("1.00")))));

        assertEquals(warehouseId, salesOrderRepository.findById(salesOrderId).orElseThrow().getConfirmedWarehouseId());
        assertEquals(warehouseId, salesReturnRepository.findById(salesReturnId).orElseThrow().getWarehouseId());
    }

    private static Product activeProduct(UUID productId) {
        Product product = new Product();
        product.setId(productId);
        product.setActive(true);
        return product;
    }

    private static Warehouse activeWarehouse(UUID warehouseId) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setCode("WH-" + warehouseId.toString().substring(0, 8));
        warehouse.setName("Main Warehouse");
        warehouse.setActive(true);
        return warehouse;
    }

    private static Customer activeCustomer(UUID customerId) {
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setCode("CUS-" + customerId.toString().substring(0, 8));
        customer.setName("Default Customer");
        customer.setActive(true);
        return customer;
    }

    private static InventoryStock stock(
            UUID productId,
            UUID warehouseId,
            String quantityOnHand,
            String reservedQuantity) {
        InventoryStock stock = new InventoryStock();
        stock.setId(UUID.randomUUID());
        stock.setProductId(productId);
        stock.setWarehouseId(warehouseId);
        stock.setQuantityOnHand(new BigDecimal(quantityOnHand));
        stock.setReservedQuantity(new BigDecimal(reservedQuantity));
        stock.setReorderLevel(BigDecimal.ZERO);
        stock.setUpdatedAt(Instant.now());
        return stock;
    }

    private static SalesOrder salesOrder(
            UUID salesOrderId,
            SalesOrderStatus status) {
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setId(salesOrderId);
        salesOrder.setStatus(status);
        return salesOrder;
    }

    private static SalesOrderItem salesOrderItem(
            UUID salesOrderId,
            UUID productId,
            String quantity,
            String unitPrice) {
        SalesOrderItem item = new SalesOrderItem();
        item.setId(UUID.randomUUID());
        item.setSalesOrderId(salesOrderId);
        item.setProductId(productId);
        item.setQuantity(new BigDecimal(quantity));
        item.setUnitPrice(new BigDecimal(unitPrice));
        return item;
    }

    private static final class InMemoryInventoryStockRepository
            implements InventoryStockRepository {
        private final Map<String, InventoryStock> stocks = new HashMap<>();

        @Override
        public InventoryStock save(InventoryStock stock) {
            if (stock.getId() == null) {
                stock.setId(UUID.randomUUID());
            }
            stocks.put(key(stock.getProductId(), stock.getWarehouseId()), stock);
            return stock;
        }

        @Override
        public List<InventoryStock> findAll() {
            return stocks.values().stream().toList();
        }

        @Override
        public Optional<InventoryStock> findByProductIdAndWarehouseId(
                UUID productId,
                UUID warehouseId) {
            return Optional.ofNullable(stocks.get(key(productId, warehouseId)));
        }

        @Override
        public List<InventoryStock> findByProductId(UUID productId) {
            return stocks.values().stream()
                    .filter(stock -> stock.getProductId().equals(productId))
                    .toList();
        }

        @Override
        public List<InventoryStock> findByWarehouseId(UUID warehouseId) {
            return stocks.values().stream()
                    .filter(stock -> stock.getWarehouseId().equals(warehouseId))
                    .toList();
        }

        private String key(UUID productId, UUID warehouseId) {
            return productId + ":" + warehouseId;
        }
    }

    private static final class InMemoryStockMovementRepository
            implements StockMovementRepository {
        private final List<StockMovement> movements = new ArrayList<>();

        @Override
        public StockMovement save(StockMovement movement) {
            if (movement.getId() == null) {
                movement.setId(UUID.randomUUID());
            }
            movements.add(movement);
            return movement;
        }

        @Override
        public List<StockMovement> findAll() {
            return new ArrayList<>(movements);
        }

        @Override
        public List<StockMovement> findByProductId(UUID productId) {
            return movements.stream()
                    .filter(movement -> movement.getProductId().equals(productId))
                    .toList();
        }

        @Override
        public List<StockMovement> findByWarehouseId(UUID warehouseId) {
            return movements.stream()
                    .filter(movement -> movement.getWarehouseId().equals(warehouseId))
                    .toList();
        }

        @Override
        public List<StockMovement> findByProjectId(UUID projectId) {
            return movements.stream()
                    .filter(movement -> projectId.equals(movement.getProjectId()))
                    .toList();
        }

        @Override
        public List<StockMovement> findByReferenceId(UUID referenceId) {
            return movements.stream()
                    .filter(movement -> movement.getReferenceId().equals(referenceId))
                    .toList();
        }

        @Override
        public List<StockMovement> findBetweenDates(
                Instant start,
                Instant end) {
            return movements.stream()
                    .filter(movement -> !movement.getMovementDate().isBefore(start))
                    .filter(movement -> !movement.getMovementDate().isAfter(end))
                    .toList();
        }

        List<StockMovement> movements() {
            return movements;
        }
    }

    private static final class InMemorySalesOrderRepository
            implements SalesOrderRepository {
        private final Map<UUID, SalesOrder> salesOrders = new HashMap<>();

        @Override
        public SalesOrder save(SalesOrder salesOrder) {
            salesOrders.put(salesOrder.getId(), salesOrder);
            return salesOrder;
        }

        @Override
        public Optional<SalesOrder> findById(UUID id) {
            return Optional.ofNullable(salesOrders.get(id));
        }

        @Override
        public Optional<SalesOrder> findBySoNumber(String soNumber) {
            return salesOrders.values().stream()
                    .filter(order -> soNumber.equals(order.getSoNumber()))
                    .findFirst();
        }

        @Override
        public List<SalesOrder> findAll() {
            return salesOrders.values().stream().toList();
        }

        @Override
        public List<SalesOrder> findByCustomerId(UUID customerId) {
            return salesOrders.values().stream()
                    .filter(order -> customerId.equals(order.getCustomerId()))
                    .toList();
        }
    }

    private static final class InMemorySalesReturnRepository implements SalesReturnRepository {
        private final Map<UUID, SalesReturn> salesReturns = new HashMap<>();

        @Override
        public SalesReturn save(SalesReturn salesReturn) {
            if (salesReturn.getId() == null) {
                salesReturn.setId(UUID.randomUUID());
            }
            salesReturns.put(salesReturn.getId(), salesReturn);
            return salesReturn;
        }

        @Override
        public Optional<SalesReturn> findById(UUID id) {
            return Optional.ofNullable(salesReturns.get(id));
        }

        @Override
        public Optional<SalesReturn> findByReturnNumber(String returnNumber) {
            return salesReturns.values().stream().filter(item -> returnNumber.equals(item.getReturnNumber())).findFirst();
        }

        @Override
        public List<SalesReturn> findAll() {
            return salesReturns.values().stream().toList();
        }

        @Override
        public List<SalesReturn> findBySalesOrderId(UUID salesOrderId) {
            return salesReturns.values().stream()
                    .filter(item -> salesOrderId.equals(item.getSalesOrderId()))
                    .toList();
        }
    }

    private static final class InMemorySalesReturnItemRepository implements SalesReturnItemRepository {
        private final Map<UUID, List<SalesReturnItem>> itemsByReturnId = new HashMap<>();

        @Override
        public SalesReturnItem save(SalesReturnItem item) {
            if (item.getId() == null) {
                item.setId(UUID.randomUUID());
            }
            itemsByReturnId.computeIfAbsent(item.getSalesReturnId(), ignored -> new ArrayList<>()).add(item);
            return item;
        }

        @Override
        public List<SalesReturnItem> findBySalesReturnId(UUID salesReturnId) {
            return new ArrayList<>(itemsByReturnId.getOrDefault(salesReturnId, List.of()));
        }
    }

    private static final class InMemorySalesOrderItemRepository
            implements SalesOrderItemRepository {
        private final Map<UUID, List<SalesOrderItem>> itemsByOrderId = new HashMap<>();

        @Override
        public SalesOrderItem save(SalesOrderItem item) {
            itemsByOrderId
                    .computeIfAbsent(item.getSalesOrderId(), ignored -> new ArrayList<>())
                    .add(item);
            return item;
        }

        @Override
        public List<SalesOrderItem> findBySalesOrderId(UUID salesOrderId) {
            return new ArrayList<>(
                    itemsByOrderId.getOrDefault(salesOrderId, List.of()));
        }

        @Override
        public void deleteBySalesOrderId(UUID salesOrderId) {
            itemsByOrderId.remove(salesOrderId);
        }
    }

    private static final class InMemoryProductRepository
            implements ProductRepository {
        private final Map<UUID, Product> products = new HashMap<>();

        @Override
        public Product save(Product product) {
            if (product.getId() == null) {
                product.setId(UUID.randomUUID());
            }
            products.put(product.getId(), product);
            return product;
        }

        @Override
        public Optional<Product> findById(UUID id) {
            return Optional.ofNullable(products.get(id));
        }

        @Override
        public Optional<Product> findBySku(String sku) {
            return products.values().stream()
                    .filter(product -> sku.equals(product.getSku()))
                    .findFirst();
        }

        @Override
        public List<Product> findByCategoryId(UUID categoryId) {
            return products.values().stream()
                    .filter(product -> categoryId.equals(product.getCategoryId()))
                    .toList();
        }

        @Override
        public List<Product> findByUnitId(UUID unitId) {
            return products.values().stream()
                    .filter(product -> unitId.equals(product.getUnitId()))
                    .toList();
        }

        @Override
        public List<Product> findAll() {
            return products.values().stream().toList();
        }

        @Override
        public List<Product> findActiveProducts() {
            return products.values().stream()
                    .filter(product -> Boolean.TRUE.equals(product.getActive()))
                    .toList();
        }

        @Override
        public void deleteById(UUID id) {
            products.remove(id);
        }

        @Override
        public boolean existsBySku(String sku) {
            return products.values().stream()
                    .anyMatch(product -> sku.equals(product.getSku()));
        }
    }

    private static final class InMemoryWarehouseRepository
            implements WarehouseRepository {
        private final Map<UUID, Warehouse> warehouses = new HashMap<>();

        @Override
        public Warehouse save(Warehouse warehouse) {
            if (warehouse.getId() == null) {
                warehouse.setId(UUID.randomUUID());
            }
            warehouses.put(warehouse.getId(), warehouse);
            return warehouse;
        }

        @Override
        public Optional<Warehouse> findById(UUID id) {
            return Optional.ofNullable(warehouses.get(id));
        }

        @Override
        public Optional<Warehouse> findByCode(String code) {
            return warehouses.values().stream()
                    .filter(warehouse -> code.equals(warehouse.getCode()))
                    .findFirst();
        }

        @Override
        public List<Warehouse> findAll() {
            return warehouses.values().stream().toList();
        }

        @Override
        public List<Warehouse> findActiveWarehouses() {
            return warehouses.values().stream()
                    .filter(warehouse -> Boolean.TRUE.equals(warehouse.getActive()))
                    .toList();
        }
    }

    private static final class InMemoryCustomerRepository
            implements CustomerRepository {
        private final Map<UUID, Customer> customers = new HashMap<>();

        @Override
        public Customer save(Customer customer) {
            if (customer.getId() == null) {
                customer.setId(UUID.randomUUID());
            }
            customers.put(customer.getId(), customer);
            return customer;
        }

        @Override
        public Optional<Customer> findById(UUID id) {
            return Optional.ofNullable(customers.get(id));
        }

        @Override
        public Optional<Customer> findByCode(String code) {
            return customers.values().stream()
                    .filter(customer -> code.equals(customer.getCode()))
                    .findFirst();
        }

        @Override
        public List<Customer> findAll() {
            return customers.values().stream().toList();
        }

        @Override
        public List<Customer> findActiveCustomers() {
            return customers.values().stream()
                    .filter(customer -> Boolean.TRUE.equals(customer.getActive()))
                    .toList();
        }
    }
}
