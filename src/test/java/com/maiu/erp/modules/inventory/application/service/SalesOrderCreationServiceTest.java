package com.maiu.erp.modules.inventory.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.inventory.application.dto.CreateSalesOrderItemRequest;
import com.maiu.erp.modules.inventory.application.dto.CreateSalesOrderRequest;
import com.maiu.erp.modules.inventory.domain.enums.SalesOrderStatus;
import com.maiu.erp.modules.inventory.domain.model.Customer;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.SalesOrder;
import com.maiu.erp.modules.inventory.domain.model.SalesOrderItem;
import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderRepository;

import java.time.LocalDate;

class SalesOrderCreationServiceTest {

    @Test
    void createSalesOrderPersistsHeaderAndItems() {
        InMemorySalesOrderRepository salesOrderRepository = new InMemorySalesOrderRepository();
        InMemorySalesOrderItemRepository itemRepository = new InMemorySalesOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();

        UUID customerId = UUID.randomUUID();
        UUID productOneId = UUID.randomUUID();
        UUID productTwoId = UUID.randomUUID();
        customerRepository.save(activeCustomer(customerId));
        productRepository.save(activeProduct(productOneId));
        productRepository.save(activeProduct(productTwoId));

        SalesOrderService salesOrderService = new SalesOrderService(
                salesOrderRepository,
                itemRepository,
                null,
                null,
                productRepository,
                customerRepository);

        UUID salesOrderId = salesOrderService.createSalesOrder(
                new CreateSalesOrderRequest(
                        customerId,
                        LocalDate.of(2026, 5, 21),
                        List.of(
                                new CreateSalesOrderItemRequest(
                                        productOneId,
                                        new BigDecimal("2"),
                                        new BigDecimal("15.50")),
                                new CreateSalesOrderItemRequest(
                                        productTwoId,
                                        new BigDecimal("3"),
                                        new BigDecimal("8.00")))));

        SalesOrder salesOrder = salesOrderRepository.findById(salesOrderId)
                .orElseThrow();
        List<SalesOrderItem> items = itemRepository.findBySalesOrderId(salesOrderId);

        assertNotNull(salesOrder.getSoNumber());
        assertEquals(SalesOrderStatus.DRAFT, salesOrder.getStatus());
        assertEquals(new BigDecimal("55.00"), salesOrder.getTotalAmount());
        assertEquals(2, items.size());
        assertEquals(new BigDecimal("31.00"), items.getFirst().getLineTotal());
    }

    @Test
    void createSalesOrderFailsWhenProductDoesNotExist() {
        InMemorySalesOrderRepository salesOrderRepository = new InMemorySalesOrderRepository();
        InMemorySalesOrderItemRepository itemRepository = new InMemorySalesOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        UUID customerId = UUID.randomUUID();
        customerRepository.save(activeCustomer(customerId));

        SalesOrderService salesOrderService = new SalesOrderService(
                salesOrderRepository,
                itemRepository,
                null,
                null,
                productRepository,
                customerRepository);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> salesOrderService.createSalesOrder(
                        new CreateSalesOrderRequest(
                                customerId,
                                LocalDate.of(2026, 5, 21),
                                List.of(new CreateSalesOrderItemRequest(
                                        UUID.randomUUID(),
                                        BigDecimal.ONE,
                                        BigDecimal.TEN)))));

        assertEquals("Product not found", exception.getMessage());
    }

    @Test
    void createSalesOrderFailsWhenCustomerDoesNotExist() {
        InMemorySalesOrderRepository salesOrderRepository = new InMemorySalesOrderRepository();
        InMemorySalesOrderItemRepository itemRepository = new InMemorySalesOrderItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();

        UUID productId = UUID.randomUUID();
        productRepository.save(activeProduct(productId));

        SalesOrderService salesOrderService = new SalesOrderService(
                salesOrderRepository,
                itemRepository,
                null,
                null,
                productRepository,
                customerRepository);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> salesOrderService.createSalesOrder(
                        new CreateSalesOrderRequest(
                                UUID.randomUUID(),
                                LocalDate.of(2026, 5, 21),
                                List.of(new CreateSalesOrderItemRequest(
                                        productId,
                                        BigDecimal.ONE,
                                        BigDecimal.TEN)))));

        assertEquals("Customer not found", exception.getMessage());
    }

    private static Product activeProduct(UUID productId) {
        Product product = new Product();
        product.setId(productId);
        product.setActive(true);
        return product;
    }

    private static Customer activeCustomer(UUID customerId) {
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setCode("CUS-" + customerId.toString().substring(0, 8));
        customer.setName("Default Customer");
        customer.setActive(true);
        return customer;
    }

    private static final class InMemorySalesOrderRepository
            implements SalesOrderRepository {
        private final Map<UUID, SalesOrder> salesOrders = new HashMap<>();

        @Override
        public SalesOrder save(SalesOrder salesOrder) {
            if (salesOrder.getId() == null) {
                salesOrder.setId(UUID.randomUUID());
            }
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

    private static final class InMemorySalesOrderItemRepository
            implements SalesOrderItemRepository {
        private final Map<UUID, List<SalesOrderItem>> itemsByOrderId = new HashMap<>();

        @Override
        public SalesOrderItem save(SalesOrderItem item) {
            if (item.getId() == null) {
                item.setId(UUID.randomUUID());
            }
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
