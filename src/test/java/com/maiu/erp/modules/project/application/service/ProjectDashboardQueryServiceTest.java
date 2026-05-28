package com.maiu.erp.modules.project.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.inventory.domain.enums.MovementType;
import com.maiu.erp.modules.inventory.domain.model.MaterialIssue;
import com.maiu.erp.modules.inventory.domain.model.MaterialIssueItem;
import com.maiu.erp.modules.inventory.domain.model.MaterialReturn;
import com.maiu.erp.modules.inventory.domain.model.MaterialReturnItem;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.StockMovement;
import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialReturnItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialReturnRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.StockMovementRepository;
import com.maiu.erp.modules.project.application.dto.ProjectDashboardDto;
import com.maiu.erp.modules.project.domain.enums.ProjectStatus;
import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.modules.project.domain.model.ProjectBudgetLine;
import com.maiu.erp.modules.project.domain.repository.ProjectBudgetLineRepository;
import com.maiu.erp.modules.project.domain.repository.ProjectRepository;

class ProjectDashboardQueryServiceTest {

    @Test
    void getProjectDashboardAggregatesCostQuantityAndActivity() {
        UUID projectId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryProjectBudgetLineRepository projectBudgetLineRepository = new InMemoryProjectBudgetLineRepository();
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        InMemoryMaterialIssueRepository materialIssueRepository = new InMemoryMaterialIssueRepository();
        InMemoryMaterialIssueItemRepository materialIssueItemRepository = new InMemoryMaterialIssueItemRepository();
        InMemoryMaterialReturnRepository materialReturnRepository = new InMemoryMaterialReturnRepository();
        InMemoryMaterialReturnItemRepository materialReturnItemRepository = new InMemoryMaterialReturnItemRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();

        Project project = new Project();
        project.setId(projectId);
        project.setProjectCode("PRJ-001");
        project.setProjectName("Tower A");
        project.setCustomerId(UUID.randomUUID());
        project.setStatus(ProjectStatus.ACTIVE);
        project.setBudgetAmount(new BigDecimal("1000.00"));
        projectRepository.save(project);

        ProjectBudgetLine budgetLine = new ProjectBudgetLine();
        budgetLine.setId(UUID.randomUUID());
        budgetLine.setProjectId(projectId);
        budgetLine.setCostCode("MAT");
        budgetLine.setDescription("Materials");
        budgetLine.setBudgetAmount(new BigDecimal("800.00"));
        projectBudgetLineRepository.save(budgetLine);

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(UUID.randomUUID());
        purchaseOrder.setProjectId(projectId);
        purchaseOrder.setTotalAmount(new BigDecimal("300.00"));
        purchaseOrderRepository.save(purchaseOrder);

        MaterialIssue materialIssue = new MaterialIssue();
        materialIssue.setId(UUID.randomUUID());
        materialIssue.setProjectId(projectId);
        materialIssueRepository.save(materialIssue);

        MaterialIssueItem issueItem = new MaterialIssueItem();
        issueItem.setId(UUID.randomUUID());
        issueItem.setMaterialIssueId(materialIssue.getId());
        issueItem.setProductId(productId);
        issueItem.setQuantity(new BigDecimal("5.00"));
        issueItem.setUnitCost(new BigDecimal("20.00"));
        issueItem.setLineTotal(new BigDecimal("100.00"));
        materialIssueItemRepository.save(issueItem);

        MaterialReturn materialReturn = new MaterialReturn();
        materialReturn.setId(UUID.randomUUID());
        materialReturn.setMaterialIssueId(materialIssue.getId());
        materialReturn.setProjectId(projectId);
        materialReturn.setWarehouseId(UUID.randomUUID());
        materialReturn.setReversal(true);
        materialReturnRepository.save(materialReturn);

        MaterialReturnItem returnItem = new MaterialReturnItem();
        returnItem.setId(UUID.randomUUID());
        returnItem.setMaterialReturnId(materialReturn.getId());
        returnItem.setProductId(productId);
        returnItem.setQuantity(new BigDecimal("1.50"));
        returnItem.setUnitCost(new BigDecimal("20.00"));
        returnItem.setLineTotal(new BigDecimal("30.00"));
        materialReturnItemRepository.save(returnItem);

        Product product = new Product();
        product.setId(productId);
        product.setName("Cement");
        productRepository.save(product);

        StockMovement issueMovement = new StockMovement();
        issueMovement.setId(UUID.randomUUID());
        issueMovement.setProjectId(projectId);
        issueMovement.setMovementType(MovementType.PROJECT_ISSUE);
        issueMovement.setQuantity(new BigDecimal("5.00"));
        issueMovement.setMovementDate(Instant.parse("2026-05-26T07:00:00Z"));
        stockMovementRepository.save(issueMovement);

        StockMovement returnMovement = new StockMovement();
        returnMovement.setId(UUID.randomUUID());
        returnMovement.setProjectId(projectId);
        returnMovement.setMovementType(MovementType.RETURN);
        returnMovement.setQuantity(new BigDecimal("1.50"));
        returnMovement.setMovementDate(Instant.parse("2026-05-26T08:00:00Z"));
        stockMovementRepository.save(returnMovement);

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
        ProjectDashboardQueryService service = new ProjectDashboardQueryService(
                projectService,
                projectCostQueryService,
                materialIssueRepository,
                materialIssueItemRepository,
                materialReturnRepository,
                materialReturnItemRepository,
                stockMovementRepository);

        ProjectDashboardDto dashboard = service.getProjectDashboard(projectId);

        assertEquals(ProjectStatus.ACTIVE, dashboard.getStatus());
        assertEquals(new BigDecimal("800.00"), dashboard.getBudgetBasisAmount());
        assertEquals(new BigDecimal("300.00"), dashboard.getCommittedCost());
        assertEquals(new BigDecimal("70.00"), dashboard.getActualCost());
        assertEquals(new BigDecimal("8.75"), dashboard.getBudgetUtilizationPercent());
        assertEquals(new BigDecimal("5.00"), dashboard.getTotalIssuedQuantity());
        assertEquals(new BigDecimal("1.50"), dashboard.getTotalReturnedQuantity());
        assertEquals(new BigDecimal("3.50"), dashboard.getNetIssuedQuantity());
        assertEquals(1, dashboard.getTotalIssueCount());
        assertEquals(1, dashboard.getTotalReturnCount());
        assertEquals(1, dashboard.getTotalReversalCount());
        assertEquals(2, dashboard.getMovementCount());
        assertEquals(Instant.parse("2026-05-26T08:00:00Z"), dashboard.getLastMovementDate());
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
            return projects.values().stream().filter(project -> projectCode.equals(project.getProjectCode())).findFirst();
        }

        @Override
        public List<Project> findAll() {
            return projects.values().stream().toList();
        }
    }

    private static final class InMemoryProjectBudgetLineRepository implements ProjectBudgetLineRepository {
        private final Map<UUID, ProjectBudgetLine> budgetLines = new HashMap<>();

        @Override
        public ProjectBudgetLine save(ProjectBudgetLine budgetLine) {
            if (budgetLine.getId() == null) {
                budgetLine.setId(UUID.randomUUID());
            }
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
                    .filter(item -> projectId.equals(item.getProjectId()) && costCode.equals(item.getCostCode()))
                    .findFirst();
        }

        @Override
        public List<ProjectBudgetLine> findByProjectId(UUID projectId) {
            return budgetLines.values().stream().filter(item -> projectId.equals(item.getProjectId())).toList();
        }

        @Override
        public void deleteById(UUID id) {
            budgetLines.remove(id);
        }
    }

    private static final class InMemoryCustomerRepository implements CustomerRepository {
        @Override
        public com.maiu.erp.modules.inventory.domain.model.Customer save(
                com.maiu.erp.modules.inventory.domain.model.Customer customer) {
            return customer;
        }

        @Override
        public Optional<com.maiu.erp.modules.inventory.domain.model.Customer> findById(UUID id) {
            return Optional.of(new com.maiu.erp.modules.inventory.domain.model.Customer());
        }

        @Override
        public Optional<com.maiu.erp.modules.inventory.domain.model.Customer> findByCode(String code) {
            return Optional.empty();
        }

        @Override
        public List<com.maiu.erp.modules.inventory.domain.model.Customer> findAll() {
            return List.of();
        }

        @Override
        public List<com.maiu.erp.modules.inventory.domain.model.Customer> findActiveCustomers() {
            return List.of();
        }
    }

    private static final class InMemoryPurchaseOrderRepository implements PurchaseOrderRepository {
        private final Map<UUID, PurchaseOrder> purchaseOrders = new HashMap<>();

        @Override
        public PurchaseOrder save(PurchaseOrder purchaseOrder) {
            purchaseOrders.put(purchaseOrder.getId(), purchaseOrder);
            return purchaseOrder;
        }

        @Override
        public Optional<PurchaseOrder> findById(UUID id) {
            return Optional.ofNullable(purchaseOrders.get(id));
        }

        @Override
        public Optional<PurchaseOrder> findByPoNumber(String poNumber) {
            return Optional.empty();
        }

        @Override
        public List<PurchaseOrder> findAll() {
            return purchaseOrders.values().stream().toList();
        }

        @Override
        public List<PurchaseOrder> findBySupplierId(UUID supplierId) {
            return List.of();
        }

        @Override
        public List<PurchaseOrder> findByProjectId(UUID projectId) {
            return purchaseOrders.values().stream()
                    .filter(order -> projectId.equals(order.getProjectId()))
                    .toList();
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
            return returns.values().stream()
                    .filter(item -> materialIssueId.equals(item.getMaterialIssueId()))
                    .toList();
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
            return Optional.empty();
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

    private static final class InMemoryStockMovementRepository implements StockMovementRepository {
        private final Map<UUID, StockMovement> movements = new HashMap<>();

        @Override
        public StockMovement save(StockMovement movement) {
            if (movement.getId() == null) {
                movement.setId(UUID.randomUUID());
            }
            movements.put(movement.getId(), movement);
            return movement;
        }

        @Override
        public List<StockMovement> findAll() {
            return movements.values().stream().toList();
        }

        @Override
        public List<StockMovement> findByProductId(UUID productId) {
            return List.of();
        }

        @Override
        public List<StockMovement> findByWarehouseId(UUID warehouseId) {
            return List.of();
        }

        @Override
        public List<StockMovement> findByProjectId(UUID projectId) {
            return movements.values().stream().filter(item -> projectId.equals(item.getProjectId())).toList();
        }

        @Override
        public List<StockMovement> findByReferenceId(UUID referenceId) {
            return List.of();
        }

        @Override
        public List<StockMovement> findBetweenDates(Instant start, Instant end) {
            return List.of();
        }
    }
}
