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
import com.maiu.erp.modules.inventory.domain.enums.MovementType;
import com.maiu.erp.modules.inventory.domain.model.Customer;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.model.MaterialIssue;
import com.maiu.erp.modules.inventory.domain.model.MaterialIssueItem;
import com.maiu.erp.modules.inventory.domain.model.MaterialReturn;
import com.maiu.erp.modules.inventory.domain.model.MaterialReturnItem;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrderItem;
import com.maiu.erp.modules.inventory.domain.model.SalesOrder;
import com.maiu.erp.modules.inventory.domain.model.SalesOrderItem;
import com.maiu.erp.modules.inventory.domain.model.SalesReturn;
import com.maiu.erp.modules.inventory.domain.model.StockMovement;
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
import com.maiu.erp.modules.project.application.dto.ProjectCostSummaryDto;
import com.maiu.erp.modules.project.application.service.ProjectCostQueryService;
import com.maiu.erp.modules.project.application.service.ProjectService;
import com.maiu.erp.modules.project.domain.enums.ProjectStatus;
import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.modules.project.domain.model.ProjectBudgetLine;
import com.maiu.erp.modules.project.domain.repository.ProjectBudgetLineRepository;
import com.maiu.erp.modules.project.domain.repository.ProjectRepository;
import com.maiu.erp.modules.reporting.application.dto.ReportingOverviewDto;

class ReportingQueryServiceTest {

    @Test
    void getOverviewAggregatesInventoryMovementAndProjectRiskData() {
        UUID projectId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

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

        Customer customer = new Customer();
        customer.setId(customerId);
        customerRepository.save(customer);

        Product product = new Product();
        product.setId(productId);
        product.setName("Cement");
        product.setSku("CEM-001");
        product.setActive(true);
        productRepository.save(product);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setCode("WH-001");
        warehouse.setName("Main Warehouse");
        warehouse.setActive(true);
        warehouseRepository.save(warehouse);

        Project project = new Project();
        project.setId(projectId);
        project.setProjectCode("PRJ-001");
        project.setProjectName("Cebu Fitout");
        project.setCustomerId(customerId);
        project.setStatus(ProjectStatus.ACTIVE);
        project.setBudgetAmount(new BigDecimal("100.00"));
        projectRepository.save(project);

        InventoryStock stock = new InventoryStock();
        stock.setId(UUID.randomUUID());
        stock.setProductId(productId);
        stock.setWarehouseId(warehouseId);
        stock.setQuantityOnHand(new BigDecimal("10.00"));
        stock.setReservedQuantity(new BigDecimal("2.00"));
        stock.setReorderLevel(BigDecimal.ZERO);
        stock.setUpdatedAt(Instant.now());
        inventoryStockRepository.save(stock);

        StockMovement movement = new StockMovement();
        movement.setId(UUID.randomUUID());
        movement.setProductId(productId);
        movement.setWarehouseId(warehouseId);
        movement.setProjectId(projectId);
        movement.setMovementType(MovementType.PROJECT_ISSUE);
        movement.setQuantity(new BigDecimal("3.00"));
        movement.setUnitCost(new BigDecimal("20.00"));
        movement.setReferenceType("MATERIAL_ISSUE");
        movement.setReferenceId(UUID.randomUUID());
        movement.setPerformedBy("warehouse lead");
        movement.setMovementDate(Instant.parse("2026-06-01T08:00:00Z"));
        stockMovementRepository.save(movement);

        ProjectBudgetLine budgetLine = new ProjectBudgetLine();
        budgetLine.setId(UUID.randomUUID());
        budgetLine.setProjectId(projectId);
        budgetLine.setCostCode("MAT");
        budgetLine.setDescription("Materials");
        budgetLine.setBudgetAmount(new BigDecimal("100.00"));
        projectBudgetLineRepository.save(budgetLine);

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(UUID.randomUUID());
        purchaseOrder.setProjectId(projectId);
        purchaseOrder.setTotalAmount(new BigDecimal("120.00"));
        purchaseOrderRepository.save(purchaseOrder);

        MaterialIssue issue = new MaterialIssue();
        issue.setId(UUID.randomUUID());
        issue.setProjectId(projectId);
        materialIssueRepository.save(issue);

        MaterialIssueItem issueItem = new MaterialIssueItem();
        issueItem.setId(UUID.randomUUID());
        issueItem.setMaterialIssueId(issue.getId());
        issueItem.setProductId(productId);
        issueItem.setQuantity(new BigDecimal("3.00"));
        issueItem.setLineTotal(new BigDecimal("60.00"));
        materialIssueItemRepository.save(issueItem);

        MaterialReturn materialReturn = new MaterialReturn();
        materialReturn.setId(UUID.randomUUID());
        materialReturn.setMaterialIssueId(issue.getId());
        materialReturn.setProjectId(projectId);
        materialReturn.setWarehouseId(warehouseId);
        materialReturnRepository.save(materialReturn);

        MaterialReturnItem returnItem = new MaterialReturnItem();
        returnItem.setId(UUID.randomUUID());
        returnItem.setMaterialReturnId(materialReturn.getId());
        returnItem.setProductId(productId);
        returnItem.setQuantity(new BigDecimal("1.00"));
        returnItem.setUnitCost(new BigDecimal("20.00"));
        returnItem.setLineTotal(new BigDecimal("20.00"));
        materialReturnItemRepository.save(returnItem);

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
                customerRepository);

        ReportingOverviewDto overview = service.getOverview(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30));

        assertEquals(new BigDecimal("10.00"), overview.getInventory().getTotalOnHand());
        assertEquals(new BigDecimal("2.00"), overview.getInventory().getTotalReserved());
        assertEquals(new BigDecimal("8.00"), overview.getInventory().getTotalAvailable());
        assertEquals(1, overview.getMovement().getTotalMovementCount());
        assertEquals("PROJECT_ISSUE", overview.getMovement().getTopMovementTypes().getFirst().getMovementType());
        assertEquals("Cement", overview.getMovement().getRecentMovements().getFirst().getProductName());
        assertEquals("PRJ-001", overview.getMovement().getRecentMovements().getFirst().getProjectCode());
        assertEquals(1, overview.getProject().getActiveProjectCount());
        assertEquals(0, overview.getProject().getOverBudgetProjectCount());
        assertEquals(new BigDecimal("120.00"), overview.getProject().getTotalCommittedCost());
        assertEquals(new BigDecimal("40.00"), overview.getProject().getTotalActualCost());
        assertEquals("PRJ-001", overview.getProject().getBudgetRiskProjects().getFirst().getProjectCode());
    }

    private static final class InMemoryInventoryStockRepository implements InventoryStockRepository {
        private final Map<String, InventoryStock> stocks = new HashMap<>();

        @Override
        public InventoryStock save(InventoryStock stock) {
            stocks.put(stock.getProductId() + ":" + stock.getWarehouseId(), stock);
            return stock;
        }

        @Override
        public List<InventoryStock> findAll() {
            return stocks.values().stream().toList();
        }

        @Override
        public Optional<InventoryStock> findByProductIdAndWarehouseId(UUID productId, UUID warehouseId) {
            return Optional.ofNullable(stocks.get(productId + ":" + warehouseId));
        }

        @Override
        public List<InventoryStock> findByProductId(UUID productId) {
            return stocks.values().stream().filter(stock -> productId.equals(stock.getProductId())).toList();
        }

        @Override
        public List<InventoryStock> findByWarehouseId(UUID warehouseId) {
            return stocks.values().stream().filter(stock -> warehouseId.equals(stock.getWarehouseId())).toList();
        }
    }

    private static final class InMemoryStockMovementRepository implements StockMovementRepository {
        private final Map<UUID, StockMovement> movements = new HashMap<>();

        @Override
        public StockMovement save(StockMovement movement) {
            movements.put(movement.getId(), movement);
            return movement;
        }

        @Override
        public List<StockMovement> findAll() {
            return movements.values().stream().toList();
        }

        @Override
        public List<StockMovement> findByProductId(UUID productId) {
            return movements.values().stream().filter(movement -> productId.equals(movement.getProductId())).toList();
        }

        @Override
        public List<StockMovement> findByWarehouseId(UUID warehouseId) {
            return movements.values().stream().filter(movement -> warehouseId.equals(movement.getWarehouseId())).toList();
        }

        @Override
        public List<StockMovement> findByProjectId(UUID projectId) {
            return movements.values().stream().filter(movement -> projectId.equals(movement.getProjectId())).toList();
        }

        @Override
        public List<StockMovement> findByReferenceId(UUID referenceId) {
            return movements.values().stream().filter(movement -> referenceId.equals(movement.getReferenceId())).toList();
        }

        @Override
        public List<StockMovement> findBetweenDates(Instant start, Instant end) {
            return movements.values().stream()
                    .filter(movement -> !movement.getMovementDate().isBefore(start))
                    .filter(movement -> !movement.getMovementDate().isAfter(end))
                    .toList();
        }
    }

    private static final class InMemoryProjectRepository implements ProjectRepository {
        private final Map<UUID, Project> projects = new HashMap<>();

        @Override
        public Project save(Project project) {
            projects.put(project.getId(), project);
            return project;
        }

        @Override
        public Optional<Project> findById(UUID id) {
            return Optional.ofNullable(projects.get(id));
        }

        @Override
        public Optional<Project> findByProjectCode(String projectCode) {
            return projects.values().stream().filter(project -> projectCode.equals(project.getProjectCode())).findFirst();
        }

        @Override
        public List<Project> findAll() {
            return projects.values().stream().toList();
        }
    }

    private static final class InMemoryCustomerRepository implements CustomerRepository {
        private final Map<UUID, Customer> customers = new HashMap<>();

        @Override
        public Customer save(Customer customer) {
            customers.put(customer.getId(), customer);
            return customer;
        }

        @Override
        public Optional<Customer> findById(UUID id) {
            return Optional.ofNullable(customers.get(id));
        }

        @Override
        public Optional<Customer> findByCode(String code) {
            return Optional.empty();
        }

        @Override
        public List<Customer> findAll() {
            return customers.values().stream().toList();
        }

        @Override
        public List<Customer> findActiveCustomers() {
            return customers.values().stream().toList();
        }
    }

    private static final class InMemoryProjectBudgetLineRepository implements ProjectBudgetLineRepository {
        private final Map<UUID, ProjectBudgetLine> budgetLines = new HashMap<>();

        @Override
        public ProjectBudgetLine save(ProjectBudgetLine budgetLine) {
            budgetLines.put(budgetLine.getId(), budgetLine);
            return budgetLine;
        }

        @Override
        public Optional<ProjectBudgetLine> findById(UUID id) {
            return Optional.ofNullable(budgetLines.get(id));
        }

        @Override
        public Optional<ProjectBudgetLine> findByProjectIdAndCostCode(UUID projectId, String costCode) {
            return budgetLines.values().stream()
                    .filter(line -> projectId.equals(line.getProjectId()) && costCode.equals(line.getCostCode()))
                    .findFirst();
        }

        @Override
        public List<ProjectBudgetLine> findByProjectId(UUID projectId) {
            return budgetLines.values().stream().filter(line -> projectId.equals(line.getProjectId())).toList();
        }

        @Override
        public void deleteById(UUID id) {
            budgetLines.remove(id);
        }
    }

    private static final class InMemoryPurchaseOrderRepository implements PurchaseOrderRepository {
        private final Map<UUID, PurchaseOrder> orders = new HashMap<>();

        @Override
        public PurchaseOrder save(PurchaseOrder purchaseOrder) {
            orders.put(purchaseOrder.getId(), purchaseOrder);
            return purchaseOrder;
        }

        @Override
        public Optional<PurchaseOrder> findById(UUID id) {
            return Optional.ofNullable(orders.get(id));
        }

        @Override
        public Optional<PurchaseOrder> findByPoNumber(String poNumber) {
            return Optional.empty();
        }

        @Override
        public List<PurchaseOrder> findAll() {
            return orders.values().stream().toList();
        }

        @Override
        public List<PurchaseOrder> findBySupplierId(UUID supplierId) {
            return List.of();
        }

        @Override
        public List<PurchaseOrder> findByProjectId(UUID projectId) {
            return orders.values().stream().filter(order -> projectId.equals(order.getProjectId())).toList();
        }
    }

    private static final class InMemoryMaterialIssueRepository implements MaterialIssueRepository {
        private final Map<UUID, MaterialIssue> issues = new HashMap<>();

        @Override
        public MaterialIssue save(MaterialIssue materialIssue) {
            issues.put(materialIssue.getId(), materialIssue);
            return materialIssue;
        }

        @Override
        public Optional<MaterialIssue> findById(UUID id) {
            return Optional.ofNullable(issues.get(id));
        }

        @Override
        public Optional<MaterialIssue> findByIssueNumber(String issueNumber) {
            return Optional.empty();
        }

        @Override
        public List<MaterialIssue> findAll() {
            return issues.values().stream().toList();
        }

        @Override
        public List<MaterialIssue> findByProjectId(UUID projectId) {
            return issues.values().stream().filter(issue -> projectId.equals(issue.getProjectId())).toList();
        }
    }

    private static final class InMemoryPurchaseOrderItemRepository implements PurchaseOrderItemRepository {
        @Override
        public PurchaseOrderItem save(PurchaseOrderItem item) {
            return item;
        }

        @Override
        public List<PurchaseOrderItem> findByPurchaseOrderId(UUID purchaseOrderId) {
            return List.of();
        }

        @Override
        public void deleteByPurchaseOrderId(UUID purchaseOrderId) {
        }
    }

    private static final class InMemoryPurchaseReturnRepository implements PurchaseReturnRepository {
        @Override
        public com.maiu.erp.modules.inventory.domain.model.PurchaseReturn save(
                com.maiu.erp.modules.inventory.domain.model.PurchaseReturn purchaseReturn) {
            return purchaseReturn;
        }

        @Override
        public Optional<com.maiu.erp.modules.inventory.domain.model.PurchaseReturn> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<com.maiu.erp.modules.inventory.domain.model.PurchaseReturn> findByReturnNumber(String returnNumber) {
            return Optional.empty();
        }

        @Override
        public List<com.maiu.erp.modules.inventory.domain.model.PurchaseReturn> findAll() {
            return List.of();
        }

        @Override
        public List<com.maiu.erp.modules.inventory.domain.model.PurchaseReturn> findByPurchaseOrderId(UUID purchaseOrderId) {
            return List.of();
        }
    }

    private static final class InMemoryMaterialIssueItemRepository implements MaterialIssueItemRepository {
        private final Map<UUID, List<MaterialIssueItem>> itemsByIssueId = new HashMap<>();

        @Override
        public MaterialIssueItem save(MaterialIssueItem item) {
            itemsByIssueId.computeIfAbsent(item.getMaterialIssueId(), ignored -> new java.util.ArrayList<>()).add(item);
            return item;
        }

        @Override
        public List<MaterialIssueItem> findByMaterialIssueId(UUID materialIssueId) {
            return itemsByIssueId.getOrDefault(materialIssueId, List.of());
        }
    }

    private static final class InMemoryMaterialReturnRepository implements MaterialReturnRepository {
        private final Map<UUID, MaterialReturn> returns = new HashMap<>();

        @Override
        public MaterialReturn save(MaterialReturn materialReturn) {
            returns.put(materialReturn.getId(), materialReturn);
            return materialReturn;
        }

        @Override
        public Optional<MaterialReturn> findById(UUID id) {
            return Optional.ofNullable(returns.get(id));
        }

        @Override
        public Optional<MaterialReturn> findByReturnNumber(String returnNumber) {
            return Optional.empty();
        }

        @Override
        public List<MaterialReturn> findAll() {
            return returns.values().stream().toList();
        }

        @Override
        public List<MaterialReturn> findByMaterialIssueId(UUID materialIssueId) {
            return returns.values().stream().filter(item -> materialIssueId.equals(item.getMaterialIssueId())).toList();
        }
    }

    private static final class InMemoryMaterialReturnItemRepository implements MaterialReturnItemRepository {
        private final Map<UUID, List<MaterialReturnItem>> itemsByReturnId = new HashMap<>();

        @Override
        public MaterialReturnItem save(MaterialReturnItem item) {
            itemsByReturnId.computeIfAbsent(item.getMaterialReturnId(), ignored -> new java.util.ArrayList<>()).add(item);
            return item;
        }

        @Override
        public List<MaterialReturnItem> findByMaterialReturnId(UUID materialReturnId) {
            return itemsByReturnId.getOrDefault(materialReturnId, List.of());
        }
    }

    private static final class InMemoryProductRepository implements ProductRepository {
        private final Map<UUID, Product> products = new HashMap<>();

        @Override
        public Product save(Product product) {
            products.put(product.getId(), product);
            return product;
        }

        @Override
        public Optional<Product> findById(UUID id) {
            return Optional.ofNullable(products.get(id));
        }

        @Override
        public Optional<Product> findBySku(String sku) {
            return products.values().stream().filter(product -> sku.equals(product.getSku())).findFirst();
        }

        @Override
        public List<Product> findByCategoryId(UUID categoryId) {
            return List.of();
        }

        @Override
        public List<Product> findByUnitId(UUID unitId) {
            return List.of();
        }

        @Override
        public List<Product> findAll() {
            return products.values().stream().toList();
        }

        @Override
        public List<Product> findActiveProducts() {
            return products.values().stream().toList();
        }

        @Override
        public void deleteById(UUID id) {
            products.remove(id);
        }

        @Override
        public boolean existsBySku(String sku) {
            return false;
        }
    }

    private static final class InMemoryWarehouseRepository implements WarehouseRepository {
        private final Map<UUID, Warehouse> warehouses = new HashMap<>();

        @Override
        public Warehouse save(Warehouse warehouse) {
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
            return warehouses.values().stream().toList();
        }
    }

    private static final class InMemorySupplierRepository implements SupplierRepository {
        @Override
        public com.maiu.erp.modules.inventory.domain.model.Supplier save(
                com.maiu.erp.modules.inventory.domain.model.Supplier supplier) {
            return supplier;
        }

        @Override
        public Optional<com.maiu.erp.modules.inventory.domain.model.Supplier> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<com.maiu.erp.modules.inventory.domain.model.Supplier> findByCode(String code) {
            return Optional.empty();
        }

        @Override
        public List<com.maiu.erp.modules.inventory.domain.model.Supplier> findAll() {
            return List.of();
        }

        @Override
        public List<com.maiu.erp.modules.inventory.domain.model.Supplier> findActiveSuppliers() {
            return List.of();
        }
    }

    private static final class InMemorySalesOrderRepository implements SalesOrderRepository {
        @Override
        public SalesOrder save(SalesOrder salesOrder) {
            return salesOrder;
        }

        @Override
        public Optional<SalesOrder> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<SalesOrder> findBySoNumber(String soNumber) {
            return Optional.empty();
        }

        @Override
        public List<SalesOrder> findAll() {
            return List.of();
        }

        @Override
        public List<SalesOrder> findByCustomerId(UUID customerId) {
            return List.of();
        }
    }

    private static final class InMemorySalesOrderItemRepository implements SalesOrderItemRepository {
        @Override
        public SalesOrderItem save(SalesOrderItem item) {
            return item;
        }

        @Override
        public List<SalesOrderItem> findBySalesOrderId(UUID salesOrderId) {
            return List.of();
        }

        @Override
        public void deleteBySalesOrderId(UUID salesOrderId) {
        }
    }

    private static final class InMemorySalesReturnRepository implements SalesReturnRepository {
        @Override
        public SalesReturn save(SalesReturn salesReturn) {
            return salesReturn;
        }

        @Override
        public Optional<SalesReturn> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<SalesReturn> findByReturnNumber(String returnNumber) {
            return Optional.empty();
        }

        @Override
        public List<SalesReturn> findAll() {
            return List.of();
        }

        @Override
        public List<SalesReturn> findBySalesOrderId(UUID salesOrderId) {
            return List.of();
        }
    }
}
