package com.maiu.erp.modules.inventory.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
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
import com.maiu.erp.modules.inventory.domain.model.Supplier;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.SupplierRepository;

class PurchaseOrderServiceTest {

    @Test
    void createPurchaseOrderPersistsHeaderAndItems() {
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        InMemoryPurchaseOrderItemRepository itemRepository = new InMemoryPurchaseOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();

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
                supplierRepository);

        UUID purchaseOrderId = purchaseOrderService.createPurchaseOrder(
                new CreatePurchaseOrderRequest(
                        supplierId,
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
                supplierRepository);

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
        UUID supplierId = UUID.randomUUID();
        supplierRepository.save(activeSupplier(supplierId));

        PurchaseOrderService purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                itemRepository,
                null,
                productRepository,
                supplierRepository);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> purchaseOrderService.createPurchaseOrder(
                        new CreatePurchaseOrderRequest(
                                supplierId,
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

        UUID productId = UUID.randomUUID();
        productRepository.save(activeProduct(productId));

        PurchaseOrderService purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                itemRepository,
                null,
                productRepository,
                supplierRepository);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> purchaseOrderService.createPurchaseOrder(
                        new CreatePurchaseOrderRequest(
                                UUID.randomUUID(),
                                LocalDate.of(2026, 5, 21),
                                null,
                                List.of(new CreatePurchaseOrderItemRequest(
                                        productId,
                                        BigDecimal.ONE,
                                        BigDecimal.TEN)))));

        assertEquals("Supplier not found", exception.getMessage());
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
}
