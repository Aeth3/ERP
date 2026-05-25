package com.maiu.erp.modules.inventory.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.inventory.application.dto.CreateMaterialIssueItemRequest;
import com.maiu.erp.modules.inventory.application.dto.CreateMaterialIssueRequest;
import com.maiu.erp.modules.inventory.domain.enums.MovementType;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.model.MaterialIssue;
import com.maiu.erp.modules.inventory.domain.model.MaterialIssueItem;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.StockMovement;
import com.maiu.erp.modules.inventory.domain.model.Warehouse;
import com.maiu.erp.modules.inventory.domain.repository.InventoryStockRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialIssueRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.StockMovementRepository;
import com.maiu.erp.modules.inventory.domain.repository.WarehouseRepository;
import com.maiu.erp.modules.project.domain.enums.ProjectStatus;
import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.modules.project.domain.repository.ProjectRepository;

class MaterialIssueServiceTest {

    @Test
    void createMaterialIssueConsumesAvailableStock() {
        InMemoryMaterialIssueRepository materialIssueRepository = new InMemoryMaterialIssueRepository();
        InMemoryMaterialIssueItemRepository materialIssueItemRepository = new InMemoryMaterialIssueItemRepository();
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();

        UUID projectId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();

        projectRepository.save(activeProject(projectId));
        productRepository.save(activeProduct(productId));
        warehouseRepository.save(warehouse(warehouseId));
        inventoryStockRepository.save(stock(productId, warehouseId, "10", "2"));

        InventoryValidationService inventoryValidationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                inventoryValidationService);
        MaterialIssueService materialIssueService = new MaterialIssueService(
                materialIssueRepository,
                materialIssueItemRepository,
                inventoryService,
                inventoryValidationService,
                projectRepository);

        UUID issueId = materialIssueService.createMaterialIssue(new CreateMaterialIssueRequest(
                projectId,
                warehouseId,
                "Concrete works",
                "foreman",
                List.of(new CreateMaterialIssueItemRequest(
                        productId,
                        new BigDecimal("3"),
                        new BigDecimal("12.50")))));

        InventoryStock updatedStock = inventoryStockRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow();
        MaterialIssue issue = materialIssueRepository.findById(issueId).orElseThrow();
        MaterialIssueItem item = materialIssueItemRepository.findByMaterialIssueId(issueId).getFirst();
        StockMovement movement = stockMovementRepository.findByReferenceId(issueId).getFirst();

        assertEquals(new BigDecimal("7"), updatedStock.getQuantityOnHand());
        assertEquals(projectId, issue.getProjectId());
        assertEquals(new BigDecimal("37.50"), item.getLineTotal());
        assertEquals(MovementType.PROJECT_ISSUE, movement.getMovementType());
        assertEquals(projectId, movement.getProjectId());
    }

    @Test
    void createMaterialIssueFailsWhenProjectIsNotActive() {
        InMemoryMaterialIssueRepository materialIssueRepository = new InMemoryMaterialIssueRepository();
        InMemoryMaterialIssueItemRepository materialIssueItemRepository = new InMemoryMaterialIssueItemRepository();
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();

        UUID projectId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();

        projectRepository.save(draftProject(projectId));
        productRepository.save(activeProduct(productId));
        warehouseRepository.save(warehouse(warehouseId));
        inventoryStockRepository.save(stock(productId, warehouseId, "10", "0"));

        InventoryValidationService inventoryValidationService = new InventoryValidationService(
                inventoryStockRepository,
                productRepository,
                warehouseRepository);
        InventoryService inventoryService = new InventoryService(
                inventoryStockRepository,
                stockMovementRepository,
                inventoryValidationService);
        MaterialIssueService materialIssueService = new MaterialIssueService(
                materialIssueRepository,
                materialIssueItemRepository,
                inventoryService,
                inventoryValidationService,
                projectRepository);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> materialIssueService.createMaterialIssue(
                new CreateMaterialIssueRequest(
                        projectId,
                        warehouseId,
                        null,
                        "foreman",
                        List.of(new CreateMaterialIssueItemRequest(
                                productId,
                                BigDecimal.ONE,
                                BigDecimal.ONE)))));

        assertEquals("Project must be active", exception.getMessage());
    }

    private static Project activeProject(UUID projectId) {
        Project project = new Project();
        project.setId(projectId);
        project.setProjectCode("PRJ-" + projectId.toString().substring(0, 8));
        project.setProjectName("Tower A");
        project.setStatus(ProjectStatus.ACTIVE);
        return project;
    }

    private static Project draftProject(UUID projectId) {
        Project project = activeProject(projectId);
        project.setStatus(ProjectStatus.DRAFT);
        return project;
    }

    private static Product activeProduct(UUID productId) {
        Product product = new Product();
        product.setId(productId);
        product.setName("Cement");
        product.setActive(true);
        return product;
    }

    private static Warehouse warehouse(UUID warehouseId) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setCode("WH-1");
        warehouse.setName("Main Warehouse");
        warehouse.setActive(true);
        return warehouse;
    }

    private static InventoryStock stock(UUID productId, UUID warehouseId, String onHand, String reserved) {
        InventoryStock stock = new InventoryStock();
        stock.setId(UUID.randomUUID());
        stock.setProductId(productId);
        stock.setWarehouseId(warehouseId);
        stock.setQuantityOnHand(new BigDecimal(onHand));
        stock.setReservedQuantity(new BigDecimal(reserved));
        stock.setReorderLevel(BigDecimal.ZERO);
        stock.setUpdatedAt(Instant.now());
        return stock;
    }

    private static final class InMemoryMaterialIssueRepository implements MaterialIssueRepository {
        private final Map<UUID, MaterialIssue> issues = new HashMap<>();

        @Override
        public MaterialIssue save(MaterialIssue materialIssue) {
            if (materialIssue.getId() == null) {
                materialIssue.setId(UUID.randomUUID());
            }
            issues.put(materialIssue.getId(), materialIssue);
            return materialIssue;
        }

        @Override
        public Optional<MaterialIssue> findById(UUID id) {
            return Optional.ofNullable(issues.get(id));
        }

        @Override
        public Optional<MaterialIssue> findByIssueNumber(String issueNumber) {
            return issues.values().stream()
                    .filter(issue -> issueNumber.equals(issue.getIssueNumber()))
                    .findFirst();
        }

        @Override
        public List<MaterialIssue> findAll() {
            return issues.values().stream().toList();
        }

        @Override
        public List<MaterialIssue> findByProjectId(UUID projectId) {
            return issues.values().stream()
                    .filter(issue -> projectId.equals(issue.getProjectId()))
                    .toList();
        }
    }

    private static final class InMemoryMaterialIssueItemRepository implements MaterialIssueItemRepository {
        private final Map<UUID, List<MaterialIssueItem>> itemsByIssueId = new HashMap<>();

        @Override
        public MaterialIssueItem save(MaterialIssueItem item) {
            if (item.getId() == null) {
                item.setId(UUID.randomUUID());
            }
            itemsByIssueId.computeIfAbsent(item.getMaterialIssueId(), ignored -> new java.util.ArrayList<>()).add(item);
            return item;
        }

        @Override
        public List<MaterialIssueItem> findByMaterialIssueId(UUID materialIssueId) {
            return new java.util.ArrayList<>(itemsByIssueId.getOrDefault(materialIssueId, List.of()));
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
            return movements.values().stream().filter(movement -> productId.equals(movement.getProductId())).toList();
        }

        @Override
        public List<StockMovement> findByWarehouseId(UUID warehouseId) {
            return movements.values().stream().filter(movement -> warehouseId.equals(movement.getWarehouseId())).toList();
        }

        @Override
        public List<StockMovement> findByReferenceId(UUID referenceId) {
            return movements.values().stream().filter(movement -> referenceId.equals(movement.getReferenceId())).toList();
        }

        @Override
        public List<StockMovement> findBetweenDates(Instant start, Instant end) {
            return movements.values().stream().toList();
        }
    }

    private static final class InMemoryProductRepository implements ProductRepository {
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
            return products.values().stream().filter(product -> Boolean.TRUE.equals(product.getActive())).toList();
        }

        @Override
        public void deleteById(UUID id) {
            products.remove(id);
        }

        @Override
        public boolean existsBySku(String sku) {
            return products.values().stream().anyMatch(product -> sku.equals(product.getSku()));
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
}
