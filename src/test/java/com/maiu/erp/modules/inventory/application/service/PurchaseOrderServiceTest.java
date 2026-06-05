package com.maiu.erp.modules.inventory.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.inventory.application.dto.CreatePurchaseOrderItemRequest;
import com.maiu.erp.modules.inventory.application.dto.CreatePurchaseOrderRequest;
import com.maiu.erp.modules.inventory.domain.enums.PurchaseOrderStatus;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrderItem;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturn;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturnItem;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.model.StockMovement;
import com.maiu.erp.modules.inventory.domain.model.Supplier;
import com.maiu.erp.modules.inventory.domain.model.Warehouse;
import com.maiu.erp.modules.inventory.domain.repository.InventoryStockRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseReturnItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseReturnRepository;
import com.maiu.erp.modules.inventory.domain.repository.StockMovementRepository;
import com.maiu.erp.modules.inventory.domain.repository.SupplierRepository;
import com.maiu.erp.modules.inventory.domain.repository.WarehouseRepository;
import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.modules.project.domain.repository.ProjectRepository;

class PurchaseOrderServiceTest {

    @Test
    void createPurchaseOrderPersistsHeaderAndItems() {
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        InMemoryPurchaseOrderItemRepository itemRepository = new InMemoryPurchaseOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();

        UUID supplierId = UUID.randomUUID();
        UUID productOneId = UUID.randomUUID();
        UUID productTwoId = UUID.randomUUID();
        supplierRepository.save(activeSupplier(supplierId));
        productRepository.save(activeProduct(productOneId));
        productRepository.save(activeProduct(productTwoId));

        PurchaseOrderService purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                itemRepository,
                null,
                productRepository,
                supplierRepository,
                projectRepository,
                new InMemoryPurchaseReturnRepository(),
                new InMemoryPurchaseReturnItemRepository());

        UUID purchaseOrderId = purchaseOrderService.createPurchaseOrder(
                new CreatePurchaseOrderRequest(
                        supplierId,
                        null,
                        LocalDate.of(2026, 5, 21),
                        LocalDate.of(2026, 5, 28),
                        List.of(
                                new CreatePurchaseOrderItemRequest(
                                        productOneId,
                                        new BigDecimal("2"),
                                        new BigDecimal("10.50")),
                                new CreatePurchaseOrderItemRequest(
                                        productTwoId,
                                        new BigDecimal("3"),
                                        new BigDecimal("5.00")))));

        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow();
        List<PurchaseOrderItem> items = itemRepository.findByPurchaseOrderId(purchaseOrderId);

        assertNotNull(purchaseOrder.getPoNumber());
        assertEquals(PurchaseOrderStatus.DRAFT, purchaseOrder.getStatus());
        assertEquals(new BigDecimal("36.00"), purchaseOrder.getTotalAmount());
        assertEquals(2, items.size());
        assertEquals(new BigDecimal("21.00"), items.getFirst().getLineTotal());
    }

    @Test
    void approvePurchaseOrderMovesDraftToApproved() {
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        InMemoryPurchaseOrderItemRepository itemRepository = new InMemoryPurchaseOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(UUID.randomUUID());
        purchaseOrder.setPoNumber("PO-TEST-001");
        purchaseOrder.setStatus(PurchaseOrderStatus.DRAFT);
        purchaseOrderRepository.save(purchaseOrder);

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setId(UUID.randomUUID());
        item.setPurchaseOrderId(purchaseOrder.getId());
        item.setProductId(UUID.randomUUID());
        item.setQuantity(BigDecimal.ONE);
        item.setUnitCost(BigDecimal.TEN);
        item.setLineTotal(BigDecimal.TEN);
        itemRepository.save(item);

        PurchaseOrderService purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                itemRepository,
                null,
                productRepository,
                supplierRepository,
                projectRepository,
                new InMemoryPurchaseReturnRepository(),
                new InMemoryPurchaseReturnItemRepository());

        purchaseOrderService.approvePurchaseOrder(purchaseOrder.getId());

        assertEquals(
                PurchaseOrderStatus.APPROVED,
                purchaseOrderRepository.findById(purchaseOrder.getId()).orElseThrow().getStatus());
    }

    @Test
    void createPurchaseOrderFailsWhenProductDoesNotExist() {
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        InMemoryPurchaseOrderItemRepository itemRepository = new InMemoryPurchaseOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        UUID supplierId = UUID.randomUUID();
        supplierRepository.save(activeSupplier(supplierId));

        PurchaseOrderService purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                itemRepository,
                null,
                productRepository,
                supplierRepository,
                projectRepository,
                new InMemoryPurchaseReturnRepository(),
                new InMemoryPurchaseReturnItemRepository());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> purchaseOrderService.createPurchaseOrder(
                        new CreatePurchaseOrderRequest(
                                supplierId,
                                null,
                                LocalDate.of(2026, 5, 21),
                                null,
                                List.of(new CreatePurchaseOrderItemRequest(
                                        UUID.randomUUID(),
                                        BigDecimal.ONE,
                                        BigDecimal.TEN)))));

        assertEquals("Product not found", exception.getMessage());
    }

    @Test
    void createPurchaseOrderFailsWhenSupplierDoesNotExist() {
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        InMemoryPurchaseOrderItemRepository itemRepository = new InMemoryPurchaseOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();

        UUID productId = UUID.randomUUID();
        productRepository.save(activeProduct(productId));

        PurchaseOrderService purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                itemRepository,
                null,
                productRepository,
                supplierRepository,
                projectRepository,
                new InMemoryPurchaseReturnRepository(),
                new InMemoryPurchaseReturnItemRepository());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> purchaseOrderService.createPurchaseOrder(
                        new CreatePurchaseOrderRequest(
                                UUID.randomUUID(),
                                null,
                                LocalDate.of(2026, 5, 21),
                                null,
                                List.of(new CreatePurchaseOrderItemRequest(
                                        productId,
                                        BigDecimal.ONE,
                                        BigDecimal.TEN)))));

        assertEquals("Supplier not found", exception.getMessage());
    }

    @Test
    void cancelDraftPurchaseOrderMarksOrderCancelled() {
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        InMemoryPurchaseOrderItemRepository itemRepository = new InMemoryPurchaseOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(UUID.randomUUID());
        purchaseOrder.setPoNumber("PO-TEST-002");
        purchaseOrder.setStatus(PurchaseOrderStatus.DRAFT);
        purchaseOrderRepository.save(purchaseOrder);

        PurchaseOrderService purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                itemRepository,
                null,
                productRepository,
                supplierRepository,
                projectRepository,
                new InMemoryPurchaseReturnRepository(),
                new InMemoryPurchaseReturnItemRepository());

        purchaseOrderService.cancelPurchaseOrder(purchaseOrder.getId());

        assertEquals(
                PurchaseOrderStatus.CANCELLED,
                purchaseOrderRepository.findById(purchaseOrder.getId()).orElseThrow().getStatus());
    }

    @Test
    void cancelApprovedPurchaseOrderMarksOrderCancelled() {
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        InMemoryPurchaseOrderItemRepository itemRepository = new InMemoryPurchaseOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(UUID.randomUUID());
        purchaseOrder.setPoNumber("PO-TEST-003");
        purchaseOrder.setStatus(PurchaseOrderStatus.APPROVED);
        purchaseOrderRepository.save(purchaseOrder);

        PurchaseOrderService purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                itemRepository,
                null,
                productRepository,
                supplierRepository,
                projectRepository,
                new InMemoryPurchaseReturnRepository(),
                new InMemoryPurchaseReturnItemRepository());

        purchaseOrderService.cancelPurchaseOrder(purchaseOrder.getId());

        assertEquals(
                PurchaseOrderStatus.CANCELLED,
                purchaseOrderRepository.findById(purchaseOrder.getId()).orElseThrow().getStatus());
    }

    @Test
    void cancelReceivedPurchaseOrderFails() {
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        InMemoryPurchaseOrderItemRepository itemRepository = new InMemoryPurchaseOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(UUID.randomUUID());
        purchaseOrder.setPoNumber("PO-TEST-004");
        purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
        purchaseOrderRepository.save(purchaseOrder);

        PurchaseOrderService purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                itemRepository,
                null,
                productRepository,
                supplierRepository,
                projectRepository,
                new InMemoryPurchaseReturnRepository(),
                new InMemoryPurchaseReturnItemRepository());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> purchaseOrderService.cancelPurchaseOrder(purchaseOrder.getId()));

        assertEquals("Received purchase orders cannot be cancelled", exception.getMessage());
        assertEquals(
                PurchaseOrderStatus.RECEIVED,
                purchaseOrderRepository.findById(purchaseOrder.getId()).orElseThrow().getStatus());
    }

    @Test
    void createPurchaseOrderFailsWhenProjectDoesNotExist() {
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        InMemoryPurchaseOrderItemRepository itemRepository = new InMemoryPurchaseOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();

        UUID supplierId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        supplierRepository.save(activeSupplier(supplierId));
        productRepository.save(activeProduct(productId));

        PurchaseOrderService purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                itemRepository,
                null,
                productRepository,
                supplierRepository,
                projectRepository,
                new InMemoryPurchaseReturnRepository(),
                new InMemoryPurchaseReturnItemRepository());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> purchaseOrderService.createPurchaseOrder(
                        new CreatePurchaseOrderRequest(
                                supplierId,
                                UUID.randomUUID(),
                                LocalDate.of(2026, 5, 21),
                                null,
                                List.of(new CreatePurchaseOrderItemRequest(
                                        productId,
                                        BigDecimal.ONE,
                                        BigDecimal.TEN)))));

        assertEquals("Project not found", exception.getMessage());
    }

    @Test
    void receivePurchaseOrderFailsWhenPerformedByIsBlank() {
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        InMemoryPurchaseOrderItemRepository itemRepository = new InMemoryPurchaseOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();

        UUID purchaseOrderId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        warehouseRepository.save(activeWarehouse(warehouseId));
        productRepository.save(activeProduct(productId));

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(purchaseOrderId);
        purchaseOrder.setPoNumber("PO-TEST-005");
        purchaseOrder.setStatus(PurchaseOrderStatus.APPROVED);
        purchaseOrderRepository.save(purchaseOrder);

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setId(UUID.randomUUID());
        item.setPurchaseOrderId(purchaseOrderId);
        item.setProductId(productId);
        item.setQuantity(BigDecimal.ONE);
        item.setUnitCost(BigDecimal.TEN);
        item.setLineTotal(BigDecimal.TEN);
        itemRepository.save(item);

        InventoryValidationService inventoryValidationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                inventoryValidationService);
        PurchaseOrderService purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                itemRepository,
                inventoryService,
                productRepository,
                supplierRepository,
                projectRepository,
                new InMemoryPurchaseReturnRepository(),
                new InMemoryPurchaseReturnItemRepository());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> purchaseOrderService.receivePurchaseOrder(purchaseOrderId, warehouseId, " "));

        assertEquals("performedBy is required", exception.getMessage());
    }

    @Test
    void createPurchaseReturnReducesStockAndPersistsReturn() {
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        InMemoryPurchaseOrderItemRepository itemRepository = new InMemoryPurchaseOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();
        InMemoryPurchaseReturnRepository purchaseReturnRepository = new InMemoryPurchaseReturnRepository();
        InMemoryPurchaseReturnItemRepository purchaseReturnItemRepository = new InMemoryPurchaseReturnItemRepository();

        UUID purchaseOrderId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        warehouseRepository.save(activeWarehouse(warehouseId));
        supplierRepository.save(activeSupplier(supplierId));
        productRepository.save(activeProduct(productId));

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(purchaseOrderId);
        purchaseOrder.setPoNumber("PO-TEST-006");
        purchaseOrder.setSupplierId(supplierId);
        purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
        purchaseOrder.setReceivedWarehouseId(warehouseId);
        purchaseOrderRepository.save(purchaseOrder);

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setId(UUID.randomUUID());
        item.setPurchaseOrderId(purchaseOrderId);
        item.setProductId(productId);
        item.setQuantity(new BigDecimal("5.00"));
        item.setUnitCost(new BigDecimal("10.00"));
        item.setLineTotal(new BigDecimal("50.00"));
        itemRepository.save(item);

        InventoryStock stock = new InventoryStock();
        stock.setProductId(productId);
        stock.setWarehouseId(warehouseId);
        stock.setQuantityOnHand(new BigDecimal("5.00"));
        stock.setReservedQuantity(BigDecimal.ZERO);
        stock.setReorderLevel(BigDecimal.ZERO);
        stock.setUpdatedAt(Instant.now());
        inventoryStockRepository.save(stock);

        InventoryValidationService inventoryValidationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                inventoryValidationService);
        PurchaseOrderService purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                itemRepository,
                inventoryService,
                productRepository,
                supplierRepository,
                projectRepository,
                purchaseReturnRepository,
                purchaseReturnItemRepository);

        UUID purchaseReturnId = purchaseOrderService.createPurchaseReturn(
                purchaseOrderId,
                new com.maiu.erp.modules.inventory.application.dto.CreatePurchaseReturnRequest(
                        "Damaged items",
                        "warehouse lead",
                        List.of(new com.maiu.erp.modules.inventory.application.dto.CreatePurchaseReturnItemRequest(
                                productId,
                                new BigDecimal("2.00")))));

        PurchaseReturn purchaseReturn = purchaseReturnRepository.findById(purchaseReturnId).orElseThrow();
        assertEquals(warehouseId, purchaseReturn.getWarehouseId());
        assertEquals(new BigDecimal("3.00"), inventoryStockRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow()
                .getQuantityOnHand());
        assertEquals(1, purchaseReturnItemRepository.findByPurchaseReturnId(purchaseReturnId).size());
    }

    @Test
    void createPurchaseReturnFailsWhenQuantityExceedsReceivedQuantity() {
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        InMemoryPurchaseOrderItemRepository itemRepository = new InMemoryPurchaseOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();
        InMemoryPurchaseReturnRepository purchaseReturnRepository = new InMemoryPurchaseReturnRepository();
        InMemoryPurchaseReturnItemRepository purchaseReturnItemRepository = new InMemoryPurchaseReturnItemRepository();

        UUID purchaseOrderId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        warehouseRepository.save(activeWarehouse(warehouseId));
        supplierRepository.save(activeSupplier(supplierId));
        productRepository.save(activeProduct(productId));

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(purchaseOrderId);
        purchaseOrder.setPoNumber("PO-TEST-007");
        purchaseOrder.setSupplierId(supplierId);
        purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
        purchaseOrder.setReceivedWarehouseId(warehouseId);
        purchaseOrderRepository.save(purchaseOrder);

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setId(UUID.randomUUID());
        item.setPurchaseOrderId(purchaseOrderId);
        item.setProductId(productId);
        item.setQuantity(new BigDecimal("5.00"));
        item.setUnitCost(new BigDecimal("10.00"));
        item.setLineTotal(new BigDecimal("50.00"));
        itemRepository.save(item);

        InventoryStock stock = new InventoryStock();
        stock.setProductId(productId);
        stock.setWarehouseId(warehouseId);
        stock.setQuantityOnHand(new BigDecimal("5.00"));
        stock.setReservedQuantity(BigDecimal.ZERO);
        stock.setReorderLevel(BigDecimal.ZERO);
        stock.setUpdatedAt(Instant.now());
        inventoryStockRepository.save(stock);

        InventoryValidationService inventoryValidationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                inventoryValidationService);
        PurchaseOrderService purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                itemRepository,
                inventoryService,
                productRepository,
                supplierRepository,
                projectRepository,
                purchaseReturnRepository,
                purchaseReturnItemRepository);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> purchaseOrderService.createPurchaseReturn(
                        purchaseOrderId,
                        new com.maiu.erp.modules.inventory.application.dto.CreatePurchaseReturnRequest(
                                "Over return",
                                "warehouse lead",
                                List.of(new com.maiu.erp.modules.inventory.application.dto.CreatePurchaseReturnItemRequest(
                                        productId,
                                        new BigDecimal("6.00"))))));

        assertEquals("Purchase return quantity exceeds received quantity for product", exception.getMessage());
    }

    @Test
    void createPurchaseReturnBackfillsReceivedWarehouseFromPurchaseMovements() {
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        InMemoryPurchaseOrderItemRepository itemRepository = new InMemoryPurchaseOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();
        InMemoryPurchaseReturnRepository purchaseReturnRepository = new InMemoryPurchaseReturnRepository();
        InMemoryPurchaseReturnItemRepository purchaseReturnItemRepository = new InMemoryPurchaseReturnItemRepository();

        UUID purchaseOrderId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        warehouseRepository.save(activeWarehouse(warehouseId));
        supplierRepository.save(activeSupplier(supplierId));
        productRepository.save(activeProduct(productId));

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(purchaseOrderId);
        purchaseOrder.setPoNumber("PO-LEGACY-001");
        purchaseOrder.setSupplierId(supplierId);
        purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
        purchaseOrderRepository.save(purchaseOrder);

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setId(UUID.randomUUID());
        item.setPurchaseOrderId(purchaseOrderId);
        item.setProductId(productId);
        item.setQuantity(new BigDecimal("5.00"));
        item.setUnitCost(new BigDecimal("10.00"));
        item.setLineTotal(new BigDecimal("50.00"));
        itemRepository.save(item);

        InventoryStock stock = new InventoryStock();
        stock.setProductId(productId);
        stock.setWarehouseId(warehouseId);
        stock.setQuantityOnHand(new BigDecimal("5.00"));
        stock.setReservedQuantity(BigDecimal.ZERO);
        stock.setReorderLevel(BigDecimal.ZERO);
        stock.setUpdatedAt(Instant.now());
        inventoryStockRepository.save(stock);

        StockMovement receiveMovement = new StockMovement();
        receiveMovement.setProductId(productId);
        receiveMovement.setWarehouseId(warehouseId);
        receiveMovement.setQuantity(new BigDecimal("5.00"));
        receiveMovement.setUnitCost(new BigDecimal("10.00"));
        receiveMovement.setReferenceType("PURCHASE_ORDER");
        receiveMovement.setReferenceId(purchaseOrderId);
        receiveMovement.setMovementDate(Instant.now());
        receiveMovement.setPerformedBy("warehouse lead");
        stockMovementRepository.save(receiveMovement);

        InventoryValidationService inventoryValidationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                inventoryValidationService);
        PurchaseOrderService purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                itemRepository,
                inventoryService,
                productRepository,
                supplierRepository,
                projectRepository,
                purchaseReturnRepository,
                purchaseReturnItemRepository,
                stockMovementRepository);

        UUID purchaseReturnId = purchaseOrderService.createPurchaseReturn(
                purchaseOrderId,
                new com.maiu.erp.modules.inventory.application.dto.CreatePurchaseReturnRequest(
                        "Legacy PO return",
                        "warehouse lead",
                        List.of(new com.maiu.erp.modules.inventory.application.dto.CreatePurchaseReturnItemRequest(
                                productId,
                                new BigDecimal("1.00")))));

        assertEquals(warehouseId, purchaseOrderRepository.findById(purchaseOrderId).orElseThrow().getReceivedWarehouseId());
        assertEquals(warehouseId, purchaseReturnRepository.findById(purchaseReturnId).orElseThrow().getWarehouseId());
    }

    private static Product activeProduct(UUID productId) {
        Product product = new Product();
        product.setId(productId);
        product.setActive(true);
        return product;
    }

    private static Supplier activeSupplier(UUID supplierId) {
        Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        supplier.setCode("SUP-" + supplierId.toString().substring(0, 8));
        supplier.setName("Default Supplier");
        supplier.setActive(true);
        return supplier;
    }

    private static Warehouse activeWarehouse(UUID warehouseId) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setCode("WH-" + warehouseId.toString().substring(0, 8));
        warehouse.setName("Main Warehouse");
        warehouse.setActive(true);
        return warehouse;
    }

    private static final class InMemoryPurchaseOrderRepository
            implements PurchaseOrderRepository {
        private final Map<UUID, PurchaseOrder> purchaseOrders = new HashMap<>();

        @Override
        public PurchaseOrder save(PurchaseOrder purchaseOrder) {
            if (purchaseOrder.getId() == null) {
                purchaseOrder.setId(UUID.randomUUID());
            }
            purchaseOrders.put(purchaseOrder.getId(), purchaseOrder);
            return purchaseOrder;
        }

        @Override
        public Optional<PurchaseOrder> findById(UUID id) {
            return Optional.ofNullable(purchaseOrders.get(id));
        }

        @Override
        public Optional<PurchaseOrder> findByPoNumber(String poNumber) {
            return purchaseOrders.values().stream()
                    .filter(order -> poNumber.equals(order.getPoNumber()))
                    .findFirst();
        }

        @Override
        public List<PurchaseOrder> findAll() {
            return purchaseOrders.values().stream().toList();
        }

        @Override
        public List<PurchaseOrder> findBySupplierId(UUID supplierId) {
            return purchaseOrders.values().stream()
                    .filter(order -> supplierId.equals(order.getSupplierId()))
                    .toList();
        }

        @Override
        public List<PurchaseOrder> findByProjectId(UUID projectId) {
            return purchaseOrders.values().stream()
                    .filter(order -> projectId.equals(order.getProjectId()))
                    .toList();
        }
    }

    private static final class InMemoryPurchaseReturnRepository implements PurchaseReturnRepository {
        private final Map<UUID, PurchaseReturn> purchaseReturns = new HashMap<>();

        @Override
        public PurchaseReturn save(PurchaseReturn purchaseReturn) {
            if (purchaseReturn.getId() == null) {
                purchaseReturn.setId(UUID.randomUUID());
            }
            purchaseReturns.put(purchaseReturn.getId(), purchaseReturn);
            return purchaseReturn;
        }

        @Override
        public Optional<PurchaseReturn> findById(UUID id) {
            return Optional.ofNullable(purchaseReturns.get(id));
        }

        @Override
        public Optional<PurchaseReturn> findByReturnNumber(String returnNumber) {
            return purchaseReturns.values().stream().filter(item -> returnNumber.equals(item.getReturnNumber())).findFirst();
        }

        @Override
        public List<PurchaseReturn> findAll() {
            return purchaseReturns.values().stream().toList();
        }

        @Override
        public List<PurchaseReturn> findByPurchaseOrderId(UUID purchaseOrderId) {
            return purchaseReturns.values().stream()
                    .filter(item -> purchaseOrderId.equals(item.getPurchaseOrderId()))
                    .toList();
        }
    }

    private static final class InMemoryPurchaseReturnItemRepository implements PurchaseReturnItemRepository {
        private final Map<UUID, List<PurchaseReturnItem>> itemsByReturnId = new HashMap<>();

        @Override
        public PurchaseReturnItem save(PurchaseReturnItem item) {
            if (item.getId() == null) {
                item.setId(UUID.randomUUID());
            }
            itemsByReturnId.computeIfAbsent(item.getPurchaseReturnId(), ignored -> new ArrayList<>()).add(item);
            return item;
        }

        @Override
        public List<PurchaseReturnItem> findByPurchaseReturnId(UUID purchaseReturnId) {
            return new ArrayList<>(itemsByReturnId.getOrDefault(purchaseReturnId, List.of()));
        }
    }

    private static final class InMemoryPurchaseOrderItemRepository
            implements PurchaseOrderItemRepository {
        private final Map<UUID, List<PurchaseOrderItem>> itemsByOrderId = new HashMap<>();

        @Override
        public PurchaseOrderItem save(PurchaseOrderItem item) {
            if (item.getId() == null) {
                item.setId(UUID.randomUUID());
            }
            itemsByOrderId
                    .computeIfAbsent(item.getPurchaseOrderId(), ignored -> new ArrayList<>())
                    .add(item);
            return item;
        }

        @Override
        public List<PurchaseOrderItem> findByPurchaseOrderId(UUID purchaseOrderId) {
            return new ArrayList<>(
                    itemsByOrderId.getOrDefault(purchaseOrderId, List.of()));
        }

        @Override
        public void deleteByPurchaseOrderId(UUID purchaseOrderId) {
            itemsByOrderId.remove(purchaseOrderId);
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

    private static final class InMemorySupplierRepository
            implements SupplierRepository {
        private final Map<UUID, Supplier> suppliers = new HashMap<>();

        @Override
        public Supplier save(Supplier supplier) {
            if (supplier.getId() == null) {
                supplier.setId(UUID.randomUUID());
            }
            suppliers.put(supplier.getId(), supplier);
            return supplier;
        }

        @Override
        public Optional<Supplier> findById(UUID id) {
            return Optional.ofNullable(suppliers.get(id));
        }

        @Override
        public Optional<Supplier> findByCode(String code) {
            return suppliers.values().stream()
                    .filter(supplier -> code.equals(supplier.getCode()))
                    .findFirst();
        }

        @Override
        public List<Supplier> findAll() {
            return suppliers.values().stream().toList();
        }

        @Override
        public List<Supplier> findActiveSuppliers() {
            return suppliers.values().stream()
                    .filter(supplier -> Boolean.TRUE.equals(supplier.getActive()))
                    .toList();
        }
    }

    private static final class InMemoryProjectRepository implements ProjectRepository {
        private final Map<UUID, Project> projects = new HashMap<>();

        @Override
        public Project save(Project project) {
            if (project.getId() == null) {
                project.setId(UUID.randomUUID());
            }
            projects.put(project.getId(), project);
            return project;
        }

        @Override
        public Optional<Project> findById(UUID id) {
            return Optional.ofNullable(projects.get(id));
        }

        @Override
        public Optional<Project> findByProjectCode(String projectCode) {
            return projects.values().stream()
                    .filter(project -> projectCode.equals(project.getProjectCode()))
                    .findFirst();
        }

        @Override
        public List<Project> findAll() {
            return projects.values().stream().toList();
        }
    }

    private static final class InMemoryInventoryStockRepository implements InventoryStockRepository {
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
        public Optional<InventoryStock> findByProductIdAndWarehouseId(UUID productId, UUID warehouseId) {
            return Optional.ofNullable(stocks.get(key(productId, warehouseId)));
        }

        @Override
        public List<InventoryStock> findByProductId(UUID productId) {
            return stocks.values().stream().filter(stock -> productId.equals(stock.getProductId())).toList();
        }

        @Override
        public List<InventoryStock> findByWarehouseId(UUID warehouseId) {
            return stocks.values().stream().filter(stock -> warehouseId.equals(stock.getWarehouseId())).toList();
        }

        private String key(UUID productId, UUID warehouseId) {
            return productId + ":" + warehouseId;
        }
    }

    private static final class InMemoryStockMovementRepository implements StockMovementRepository {
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
            return movements.stream().filter(movement -> productId.equals(movement.getProductId())).toList();
        }

        @Override
        public List<StockMovement> findByWarehouseId(UUID warehouseId) {
            return movements.stream().filter(movement -> warehouseId.equals(movement.getWarehouseId())).toList();
        }

        @Override
        public List<StockMovement> findByProjectId(UUID projectId) {
            return movements.stream().filter(movement -> projectId.equals(movement.getProjectId())).toList();
        }

        @Override
        public List<StockMovement> findByReferenceId(UUID referenceId) {
            return movements.stream().filter(movement -> referenceId.equals(movement.getReferenceId())).toList();
        }

        @Override
        public List<StockMovement> findBetweenDates(Instant start, Instant end) {
            return movements.stream().toList();
        }
    }

    private static final class InMemoryWarehouseRepository implements WarehouseRepository {
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
            return warehouses.values().stream().filter(warehouse -> code.equals(warehouse.getCode())).findFirst();
        }

        @Override
        public List<Warehouse> findAll() {
            return warehouses.values().stream().toList();
        }

        @Override
        public List<Warehouse> findActiveWarehouses() {
            return warehouses.values().stream().filter(warehouse -> Boolean.TRUE.equals(warehouse.getActive())).toList();
        }
    }
}
