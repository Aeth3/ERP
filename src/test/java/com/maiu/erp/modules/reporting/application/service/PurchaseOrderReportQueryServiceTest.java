package com.maiu.erp.modules.reporting.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.inventory.application.service.InventoryService;
import com.maiu.erp.modules.inventory.application.service.InventoryValidationService;
import com.maiu.erp.modules.inventory.domain.enums.PurchaseOrderStatus;
import com.maiu.erp.modules.inventory.domain.model.Customer;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrderItem;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturn;
import com.maiu.erp.modules.inventory.domain.model.SalesOrder;
import com.maiu.erp.modules.inventory.domain.model.SalesOrderItem;
import com.maiu.erp.modules.inventory.domain.model.SalesReturn;
import com.maiu.erp.modules.inventory.domain.model.Supplier;
import com.maiu.erp.modules.inventory.domain.model.Warehouse;
import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;
import com.maiu.erp.modules.inventory.domain.repository.InventoryStockRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialReturnItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialReturnRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseReturnRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesReturnRepository;
import com.maiu.erp.modules.inventory.domain.repository.StockMovementRepository;
import com.maiu.erp.modules.inventory.domain.repository.SupplierRepository;
import com.maiu.erp.modules.inventory.domain.repository.WarehouseRepository;
import com.maiu.erp.modules.project.application.service.ProjectCostQueryService;
import com.maiu.erp.modules.project.application.service.ProjectService;
import com.maiu.erp.modules.project.domain.enums.ProjectStatus;
import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.modules.project.domain.repository.ProjectBudgetLineRepository;
import com.maiu.erp.modules.project.domain.repository.ProjectRepository;

class PurchaseOrderReportQueryServiceTest {

    @Test
    void getPurchaseOrderReportFiltersAndEnrichesOrders() {
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryProjectBudgetLineRepository projectBudgetLineRepository = new InMemoryProjectBudgetLineRepository();
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        InMemoryPurchaseOrderItemRepository purchaseOrderItemRepository = new InMemoryPurchaseOrderItemRepository();
        InMemoryPurchaseReturnRepository purchaseReturnRepository = new InMemoryPurchaseReturnRepository();
        InMemoryMaterialIssueRepository materialIssueRepository = new InMemoryMaterialIssueRepository();
        InMemoryMaterialIssueItemRepository materialIssueItemRepository = new InMemoryMaterialIssueItemRepository();
        InMemoryMaterialReturnRepository materialReturnRepository = new InMemoryMaterialReturnRepository();
        InMemoryMaterialReturnItemRepository materialReturnItemRepository = new InMemoryMaterialReturnItemRepository();
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemorySalesOrderRepository salesOrderRepository = new InMemorySalesOrderRepository();
        InMemorySalesOrderItemRepository salesOrderItemRepository = new InMemorySalesOrderItemRepository();
        InMemorySalesReturnRepository salesReturnRepository = new InMemorySalesReturnRepository();

        UUID customerId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(customerId);
        customerRepository.save(customer);

        Project project = new Project();
        project.setId(projectId);
        project.setCustomerId(customerId);
        project.setProjectCode("PRJ-001");
        project.setProjectName("Fitout");
        project.setStatus(ProjectStatus.ACTIVE);
        projectRepository.save(project);

        Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        supplier.setCode("SUP-001");
        supplier.setName("Build Supply");
        supplierRepository.save(supplier);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setCode("WH-001");
        warehouse.setName("Main");
        warehouseRepository.save(warehouse);

        PurchaseOrder matching = new PurchaseOrder();
        matching.setId(UUID.randomUUID());
        matching.setPoNumber("PO-001");
        matching.setSupplierId(supplierId);
        matching.setProjectId(projectId);
        matching.setReceivedWarehouseId(warehouseId);
        matching.setStatus(PurchaseOrderStatus.APPROVED);
        matching.setOrderDate(LocalDate.of(2026, 6, 10));
        matching.setExpectedDate(LocalDate.of(2026, 6, 20));
        matching.setTotalAmount(new BigDecimal("250.00"));
        matching.setCreatedAt(Instant.parse("2026-06-10T08:00:00Z"));
        purchaseOrderRepository.save(matching);

        PurchaseOrder other = new PurchaseOrder();
        other.setId(UUID.randomUUID());
        other.setPoNumber("PO-002");
        other.setSupplierId(supplierId);
        other.setStatus(PurchaseOrderStatus.CANCELLED);
        other.setOrderDate(LocalDate.of(2026, 5, 10));
        other.setTotalAmount(new BigDecimal("50.00"));
        other.setCreatedAt(Instant.parse("2026-05-10T08:00:00Z"));
        purchaseOrderRepository.save(other);

        PurchaseOrderItem itemOne = new PurchaseOrderItem();
        itemOne.setId(UUID.randomUUID());
        itemOne.setPurchaseOrderId(matching.getId());
        purchaseOrderItemRepository.save(itemOne);
        PurchaseOrderItem itemTwo = new PurchaseOrderItem();
        itemTwo.setId(UUID.randomUUID());
        itemTwo.setPurchaseOrderId(matching.getId());
        purchaseOrderItemRepository.save(itemTwo);

        PurchaseReturn purchaseReturn = new PurchaseReturn();
        purchaseReturn.setId(UUID.randomUUID());
        purchaseReturn.setPurchaseOrderId(matching.getId());
        purchaseReturnRepository.save(purchaseReturn);

        InventoryValidationService inventoryValidationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                inventoryValidationService);
        ProjectService projectService = new ProjectService(projectRepository, customerRepository);
        ProjectCostQueryService projectCostQueryService = new ProjectCostQueryService(
                projectService,
                projectBudgetLineRepository,
                purchaseOrderRepository,
                materialIssueRepository,
                materialIssueItemRepository,
                materialReturnRepository,
                materialReturnItemRepository,
                productRepository);
        ReportingQueryService service = new ReportingQueryService(
                inventoryService,
                projectRepository,
                productRepository,
                warehouseRepository,
                projectCostQueryService,
                purchaseOrderRepository,
                purchaseOrderItemRepository,
                purchaseReturnRepository,
                supplierRepository,
                salesOrderRepository,
                salesOrderItemRepository,
                salesReturnRepository,
                customerRepository,
                stockMovementRepository);

        var report = service.getPurchaseOrderReport(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "APPROVED",
                projectId,
                supplierId,
                0,
                20);

        assertEquals(1, report.getTotalOrders());
        assertEquals(new BigDecimal("250.00"), report.getTotalAmount());
        assertEquals(0, report.getPage());
        assertEquals(20, report.getSize());
        assertEquals("Build Supply", report.getOrders().getFirst().getSupplierName());
        assertEquals("PRJ-001", report.getOrders().getFirst().getProjectCode());
        assertEquals("WH-001", report.getOrders().getFirst().getReceivedWarehouseCode());
        assertEquals(2, report.getOrders().getFirst().getItemCount());
        assertEquals(1, report.getOrders().getFirst().getReturnCount());
    }

    private static final class InMemoryInventoryStockRepository implements InventoryStockRepository {
        @Override public InventoryStock save(InventoryStock stock) { return stock; }
        @Override public List<InventoryStock> findAll() { return List.of(); }
        @Override public Optional<InventoryStock> findByProductIdAndWarehouseId(UUID productId, UUID warehouseId) { return Optional.empty(); }
        @Override public List<InventoryStock> findByProductId(UUID productId) { return List.of(); }
        @Override public List<InventoryStock> findByWarehouseId(UUID warehouseId) { return List.of(); }
    }

    private static final class InMemoryStockMovementRepository implements StockMovementRepository {
        @Override public com.maiu.erp.modules.inventory.domain.model.StockMovement save(com.maiu.erp.modules.inventory.domain.model.StockMovement movement) { return movement; }
        @Override public List<com.maiu.erp.modules.inventory.domain.model.StockMovement> findAll() { return List.of(); }
        @Override public List<com.maiu.erp.modules.inventory.domain.model.StockMovement> findByProductId(UUID productId) { return List.of(); }
        @Override public List<com.maiu.erp.modules.inventory.domain.model.StockMovement> findByWarehouseId(UUID warehouseId) { return List.of(); }
        @Override public List<com.maiu.erp.modules.inventory.domain.model.StockMovement> findByProjectId(UUID projectId) { return List.of(); }
        @Override public List<com.maiu.erp.modules.inventory.domain.model.StockMovement> findByReferenceId(UUID referenceId) { return List.of(); }
        @Override public List<com.maiu.erp.modules.inventory.domain.model.StockMovement> findBetweenDates(Instant start, Instant end) { return List.of(); }
    }

    private static final class InMemoryProjectRepository implements ProjectRepository {
        private final Map<UUID, Project> projects = new HashMap<>();
        @Override public Project save(Project project) { projects.put(project.getId(), project); return project; }
        @Override public Optional<Project> findById(UUID id) { return Optional.ofNullable(projects.get(id)); }
        @Override public Optional<Project> findByProjectCode(String projectCode) { return projects.values().stream().filter(project -> projectCode.equals(project.getProjectCode())).findFirst(); }
        @Override public List<Project> findAll() { return projects.values().stream().toList(); }
    }

    private static final class InMemoryCustomerRepository implements CustomerRepository {
        private final Map<UUID, Customer> customers = new HashMap<>();
        @Override public Customer save(Customer customer) { customers.put(customer.getId(), customer); return customer; }
        @Override public Optional<Customer> findById(UUID id) { return Optional.ofNullable(customers.get(id)); }
        @Override public Optional<Customer> findByCode(String code) { return Optional.empty(); }
        @Override public List<Customer> findAll() { return customers.values().stream().toList(); }
        @Override public List<Customer> findActiveCustomers() { return customers.values().stream().toList(); }
    }

    private static final class InMemoryProjectBudgetLineRepository implements ProjectBudgetLineRepository {
        @Override public com.maiu.erp.modules.project.domain.model.ProjectBudgetLine save(com.maiu.erp.modules.project.domain.model.ProjectBudgetLine budgetLine) { return budgetLine; }
        @Override public Optional<com.maiu.erp.modules.project.domain.model.ProjectBudgetLine> findById(UUID id) { return Optional.empty(); }
        @Override public Optional<com.maiu.erp.modules.project.domain.model.ProjectBudgetLine> findByProjectIdAndCostCode(UUID projectId, String costCode) { return Optional.empty(); }
        @Override public List<com.maiu.erp.modules.project.domain.model.ProjectBudgetLine> findByProjectId(UUID projectId) { return List.of(); }
        @Override public void deleteById(UUID id) {}
    }

    private static final class InMemoryPurchaseOrderRepository implements PurchaseOrderRepository {
        private final Map<UUID, PurchaseOrder> orders = new HashMap<>();
        @Override public PurchaseOrder save(PurchaseOrder purchaseOrder) { orders.put(purchaseOrder.getId(), purchaseOrder); return purchaseOrder; }
        @Override public Optional<PurchaseOrder> findById(UUID id) { return Optional.ofNullable(orders.get(id)); }
        @Override public Optional<PurchaseOrder> findByPoNumber(String poNumber) { return orders.values().stream().filter(order -> poNumber.equals(order.getPoNumber())).findFirst(); }
        @Override public List<PurchaseOrder> findAll() { return orders.values().stream().toList(); }
        @Override public List<PurchaseOrder> findBySupplierId(UUID supplierId) { return orders.values().stream().filter(order -> supplierId.equals(order.getSupplierId())).toList(); }
        @Override public List<PurchaseOrder> findByProjectId(UUID projectId) { return orders.values().stream().filter(order -> projectId.equals(order.getProjectId())).toList(); }
    }

    private static final class InMemoryPurchaseOrderItemRepository implements PurchaseOrderItemRepository {
        private final Map<UUID, List<PurchaseOrderItem>> items = new HashMap<>();
        @Override public PurchaseOrderItem save(PurchaseOrderItem item) { items.computeIfAbsent(item.getPurchaseOrderId(), ignored -> new java.util.ArrayList<>()).add(item); return item; }
        @Override public List<PurchaseOrderItem> findByPurchaseOrderId(UUID purchaseOrderId) { return items.getOrDefault(purchaseOrderId, List.of()); }
        @Override public void deleteByPurchaseOrderId(UUID purchaseOrderId) {}
    }

    private static final class InMemoryPurchaseReturnRepository implements PurchaseReturnRepository {
        private final Map<UUID, PurchaseReturn> returns = new HashMap<>();
        @Override public PurchaseReturn save(PurchaseReturn purchaseReturn) { returns.put(purchaseReturn.getId(), purchaseReturn); return purchaseReturn; }
        @Override public Optional<PurchaseReturn> findById(UUID id) { return Optional.ofNullable(returns.get(id)); }
        @Override public Optional<PurchaseReturn> findByReturnNumber(String returnNumber) { return Optional.empty(); }
        @Override public List<PurchaseReturn> findAll() { return returns.values().stream().toList(); }
        @Override public List<PurchaseReturn> findByPurchaseOrderId(UUID purchaseOrderId) { return returns.values().stream().filter(value -> purchaseOrderId.equals(value.getPurchaseOrderId())).toList(); }
    }

    private static final class InMemoryMaterialIssueRepository implements MaterialIssueRepository {
        @Override public com.maiu.erp.modules.inventory.domain.model.MaterialIssue save(com.maiu.erp.modules.inventory.domain.model.MaterialIssue materialIssue) { return materialIssue; }
        @Override public Optional<com.maiu.erp.modules.inventory.domain.model.MaterialIssue> findById(UUID id) { return Optional.empty(); }
        @Override public Optional<com.maiu.erp.modules.inventory.domain.model.MaterialIssue> findByIssueNumber(String issueNumber) { return Optional.empty(); }
        @Override public List<com.maiu.erp.modules.inventory.domain.model.MaterialIssue> findAll() { return List.of(); }
        @Override public List<com.maiu.erp.modules.inventory.domain.model.MaterialIssue> findByProjectId(UUID projectId) { return List.of(); }
    }

    private static final class InMemoryMaterialIssueItemRepository implements MaterialIssueItemRepository {
        @Override public com.maiu.erp.modules.inventory.domain.model.MaterialIssueItem save(com.maiu.erp.modules.inventory.domain.model.MaterialIssueItem item) { return item; }
        @Override public List<com.maiu.erp.modules.inventory.domain.model.MaterialIssueItem> findByMaterialIssueId(UUID materialIssueId) { return List.of(); }
    }

    private static final class InMemoryMaterialReturnRepository implements MaterialReturnRepository {
        @Override public com.maiu.erp.modules.inventory.domain.model.MaterialReturn save(com.maiu.erp.modules.inventory.domain.model.MaterialReturn materialReturn) { return materialReturn; }
        @Override public Optional<com.maiu.erp.modules.inventory.domain.model.MaterialReturn> findById(UUID id) { return Optional.empty(); }
        @Override public Optional<com.maiu.erp.modules.inventory.domain.model.MaterialReturn> findByReturnNumber(String returnNumber) { return Optional.empty(); }
        @Override public List<com.maiu.erp.modules.inventory.domain.model.MaterialReturn> findAll() { return List.of(); }
        @Override public List<com.maiu.erp.modules.inventory.domain.model.MaterialReturn> findByMaterialIssueId(UUID materialIssueId) { return List.of(); }
    }

    private static final class InMemoryMaterialReturnItemRepository implements MaterialReturnItemRepository {
        @Override public com.maiu.erp.modules.inventory.domain.model.MaterialReturnItem save(com.maiu.erp.modules.inventory.domain.model.MaterialReturnItem item) { return item; }
        @Override public List<com.maiu.erp.modules.inventory.domain.model.MaterialReturnItem> findByMaterialReturnId(UUID materialReturnId) { return List.of(); }
    }

    private static final class InMemoryProductRepository implements ProductRepository {
        @Override public Product save(Product product) { return product; }
        @Override public Optional<Product> findById(UUID id) { return Optional.empty(); }
        @Override public Optional<Product> findBySku(String sku) { return Optional.empty(); }
        @Override public List<Product> findByCategoryId(UUID categoryId) { return List.of(); }
        @Override public List<Product> findByUnitId(UUID unitId) { return List.of(); }
        @Override public List<Product> findAll() { return List.of(); }
        @Override public List<Product> findActiveProducts() { return List.of(); }
        @Override public void deleteById(UUID id) {}
        @Override public boolean existsBySku(String sku) { return false; }
    }

    private static final class InMemoryWarehouseRepository implements WarehouseRepository {
        private final Map<UUID, Warehouse> warehouses = new HashMap<>();
        @Override public Warehouse save(Warehouse warehouse) { warehouses.put(warehouse.getId(), warehouse); return warehouse; }
        @Override public Optional<Warehouse> findById(UUID id) { return Optional.ofNullable(warehouses.get(id)); }
        @Override public Optional<Warehouse> findByCode(String code) { return warehouses.values().stream().filter(warehouse -> code.equals(warehouse.getCode())).findFirst(); }
        @Override public List<Warehouse> findAll() { return warehouses.values().stream().toList(); }
        @Override public List<Warehouse> findActiveWarehouses() { return warehouses.values().stream().toList(); }
    }

    private static final class InMemorySupplierRepository implements SupplierRepository {
        private final Map<UUID, Supplier> suppliers = new HashMap<>();
        @Override public Supplier save(Supplier supplier) { suppliers.put(supplier.getId(), supplier); return supplier; }
        @Override public Optional<Supplier> findById(UUID id) { return Optional.ofNullable(suppliers.get(id)); }
        @Override public Optional<Supplier> findByCode(String code) { return suppliers.values().stream().filter(supplier -> code.equals(supplier.getCode())).findFirst(); }
        @Override public List<Supplier> findAll() { return suppliers.values().stream().toList(); }
        @Override public List<Supplier> findActiveSuppliers() { return suppliers.values().stream().toList(); }
    }

    private static final class InMemorySalesOrderRepository implements SalesOrderRepository {
        @Override public SalesOrder save(SalesOrder salesOrder) { return salesOrder; }
        @Override public Optional<SalesOrder> findById(UUID id) { return Optional.empty(); }
        @Override public Optional<SalesOrder> findBySoNumber(String soNumber) { return Optional.empty(); }
        @Override public List<SalesOrder> findAll() { return List.of(); }
        @Override public List<SalesOrder> findByCustomerId(UUID customerId) { return List.of(); }
    }

    private static final class InMemorySalesOrderItemRepository implements SalesOrderItemRepository {
        @Override public SalesOrderItem save(SalesOrderItem item) { return item; }
        @Override public List<SalesOrderItem> findBySalesOrderId(UUID salesOrderId) { return List.of(); }
        @Override public void deleteBySalesOrderId(UUID salesOrderId) {}
    }

    private static final class InMemorySalesReturnRepository implements SalesReturnRepository {
        @Override public SalesReturn save(SalesReturn salesReturn) { return salesReturn; }
        @Override public Optional<SalesReturn> findById(UUID id) { return Optional.empty(); }
        @Override public Optional<SalesReturn> findByReturnNumber(String returnNumber) { return Optional.empty(); }
        @Override public List<SalesReturn> findAll() { return List.of(); }
        @Override public List<SalesReturn> findBySalesOrderId(UUID salesOrderId) { return List.of(); }
    }
}
