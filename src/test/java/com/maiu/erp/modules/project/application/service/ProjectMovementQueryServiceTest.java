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
import com.maiu.erp.modules.inventory.domain.model.MaterialReturn;
import com.maiu.erp.modules.inventory.domain.model.StockMovement;
import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;
import com.maiu.erp.modules.inventory.domain.repository.MaterialReturnRepository;
import com.maiu.erp.modules.inventory.domain.repository.StockMovementRepository;
import com.maiu.erp.modules.project.application.dto.ProjectMovementDto;
import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.modules.project.domain.repository.ProjectRepository;

class ProjectMovementQueryServiceTest {

    @Test
    void getProjectMovementsIncludesReversalFlagForMaterialReturns() {
        UUID projectId = UUID.randomUUID();
        UUID returnId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();

        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
        InMemoryMaterialReturnRepository materialReturnRepository = new InMemoryMaterialReturnRepository();

        Project project = new Project();
        project.setId(projectId);
        project.setProjectCode("PRJ-001");
        project.setProjectName("Tower A");
        project.setCustomerId(UUID.randomUUID());
        projectRepository.save(project);

        MaterialReturn reversalReturn = new MaterialReturn();
        reversalReturn.setId(returnId);
        reversalReturn.setReversal(true);
        materialReturnRepository.save(reversalReturn);

        StockMovement movement = new StockMovement();
        movement.setId(UUID.randomUUID());
        movement.setProjectId(projectId);
        movement.setProductId(productId);
        movement.setWarehouseId(warehouseId);
        movement.setMovementType(MovementType.RETURN);
        movement.setQuantity(new BigDecimal("2.00"));
        movement.setUnitCost(new BigDecimal("12.50"));
        movement.setReferenceType("MATERIAL_RETURN");
        movement.setReferenceId(returnId);
        movement.setRemarks("Reversal of MI-0003");
        movement.setPerformedBy("warehouse lead");
        movement.setMovementDate(Instant.parse("2026-05-26T07:00:00Z"));
        stockMovementRepository.save(movement);

        ProjectService projectService = new ProjectService(projectRepository, customerRepository);
        ProjectMovementQueryService service = new ProjectMovementQueryService(
                projectService,
                stockMovementRepository,
                materialReturnRepository);

        List<ProjectMovementDto> movements = service.getProjectMovements(projectId);

        assertEquals(1, movements.size());
        assertEquals(true, movements.getFirst().isReversal());
        assertEquals("MATERIAL_RETURN", movements.getFirst().getReferenceType());
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
            return movements.values().stream().filter(movement -> projectId.equals(movement.getProjectId())).toList();
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

    private static final class InMemoryMaterialReturnRepository implements MaterialReturnRepository {
        private final Map<UUID, MaterialReturn> returns = new HashMap<>();

        @Override
        public MaterialReturn save(MaterialReturn materialReturn) {
            if (materialReturn.getId() == null) {
                materialReturn.setId(UUID.randomUUID());
            }
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
            return List.of();
        }
    }
}
