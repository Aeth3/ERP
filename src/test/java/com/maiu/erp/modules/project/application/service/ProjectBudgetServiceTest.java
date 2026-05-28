package com.maiu.erp.modules.project.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.inventory.domain.model.Customer;
import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;
import com.maiu.erp.modules.project.application.dto.CreateProjectBudgetLineRequest;
import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.modules.project.domain.model.ProjectBudgetLine;
import com.maiu.erp.modules.project.domain.repository.ProjectBudgetLineRepository;
import com.maiu.erp.modules.project.domain.repository.ProjectRepository;

class ProjectBudgetServiceTest {

    @Test
    void createBudgetLineStoresNormalizedProjectBudgetLine() {
        UUID projectId = UUID.randomUUID();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryProjectBudgetLineRepository budgetLineRepository = new InMemoryProjectBudgetLineRepository();
        customerRepository.save(activeCustomer(UUID.randomUUID()));

        Project project = new Project();
        project.setId(projectId);
        project.setProjectCode("PRJ-001");
        project.setProjectName("Tower A");
        project.setCustomerId(UUID.randomUUID());
        projectRepository.save(project);

        ProjectBudgetService service = new ProjectBudgetService(
                new ProjectService(projectRepository, customerRepository),
                budgetLineRepository);

        UUID budgetLineId = service.createBudgetLine(projectId, new CreateProjectBudgetLineRequest(
                " MAT ",
                " Structural Materials ",
                new BigDecimal("500.00")));

        ProjectBudgetLine saved = budgetLineRepository.findById(budgetLineId).orElseThrow();
        assertEquals(projectId, saved.getProjectId());
        assertEquals("MAT", saved.getCostCode());
        assertEquals("Structural Materials", saved.getDescription());
        assertEquals(new BigDecimal("500.00"), saved.getBudgetAmount());
    }

    @Test
    void createBudgetLineRejectsDuplicateCostCodeWithinProject() {
        UUID projectId = UUID.randomUUID();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryProjectBudgetLineRepository budgetLineRepository = new InMemoryProjectBudgetLineRepository();
        customerRepository.save(activeCustomer(UUID.randomUUID()));

        Project project = new Project();
        project.setId(projectId);
        project.setProjectCode("PRJ-002");
        project.setProjectName("Tower B");
        project.setCustomerId(UUID.randomUUID());
        projectRepository.save(project);

        ProjectBudgetLine existing = new ProjectBudgetLine();
        existing.setId(UUID.randomUUID());
        existing.setProjectId(projectId);
        existing.setCostCode("MAT");
        existing.setDescription("Existing Materials");
        existing.setBudgetAmount(new BigDecimal("100.00"));
        budgetLineRepository.save(existing);

        ProjectBudgetService service = new ProjectBudgetService(
                new ProjectService(projectRepository, customerRepository),
                budgetLineRepository);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.createBudgetLine(
                projectId,
                new CreateProjectBudgetLineRequest("MAT", "Duplicate", new BigDecimal("50.00"))));

        assertEquals("Project budget cost code already exists", exception.getMessage());
    }

    @Test
    void updateBudgetLineChangesOwnedBudgetLine() {
        UUID projectId = UUID.randomUUID();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryProjectBudgetLineRepository budgetLineRepository = new InMemoryProjectBudgetLineRepository();
        UUID customerId = UUID.randomUUID();
        customerRepository.save(activeCustomer(customerId));

        Project project = new Project();
        project.setId(projectId);
        project.setProjectCode("PRJ-003");
        project.setProjectName("Tower C");
        project.setCustomerId(customerId);
        projectRepository.save(project);

        ProjectBudgetLine line = new ProjectBudgetLine();
        line.setId(UUID.randomUUID());
        line.setProjectId(projectId);
        line.setCostCode("MAT");
        line.setDescription("Materials");
        line.setBudgetAmount(new BigDecimal("100.00"));
        budgetLineRepository.save(line);

        ProjectBudgetService service = new ProjectBudgetService(
                new ProjectService(projectRepository, customerRepository),
                budgetLineRepository);

        service.updateBudgetLine(projectId, line.getId(), new CreateProjectBudgetLineRequest(
                "MAT-1",
                "Updated Materials",
                new BigDecimal("150.00")));

        ProjectBudgetLine updated = budgetLineRepository.findById(line.getId()).orElseThrow();
        assertEquals("MAT-1", updated.getCostCode());
        assertEquals("Updated Materials", updated.getDescription());
        assertEquals(new BigDecimal("150.00"), updated.getBudgetAmount());
    }

    @Test
    void deleteBudgetLineRemovesOwnedBudgetLine() {
        UUID projectId = UUID.randomUUID();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryProjectBudgetLineRepository budgetLineRepository = new InMemoryProjectBudgetLineRepository();
        UUID customerId = UUID.randomUUID();
        customerRepository.save(activeCustomer(customerId));

        Project project = new Project();
        project.setId(projectId);
        project.setProjectCode("PRJ-004");
        project.setProjectName("Tower D");
        project.setCustomerId(customerId);
        projectRepository.save(project);

        ProjectBudgetLine line = new ProjectBudgetLine();
        line.setId(UUID.randomUUID());
        line.setProjectId(projectId);
        line.setCostCode("LAB");
        line.setDescription("Labor");
        line.setBudgetAmount(new BigDecimal("200.00"));
        budgetLineRepository.save(line);

        ProjectBudgetService service = new ProjectBudgetService(
                new ProjectService(projectRepository, customerRepository),
                budgetLineRepository);

        service.deleteBudgetLine(projectId, line.getId());

        assertEquals(Optional.empty(), budgetLineRepository.findById(line.getId()));
    }

    @Test
    void updateBudgetLineRejectsBudgetLineFromAnotherProject() {
        UUID projectId = UUID.randomUUID();
        UUID otherProjectId = UUID.randomUUID();
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryProjectBudgetLineRepository budgetLineRepository = new InMemoryProjectBudgetLineRepository();
        UUID customerId = UUID.randomUUID();
        customerRepository.save(activeCustomer(customerId));

        Project project = new Project();
        project.setId(projectId);
        project.setProjectCode("PRJ-005");
        project.setProjectName("Tower E");
        project.setCustomerId(customerId);
        projectRepository.save(project);

        Project otherProject = new Project();
        otherProject.setId(otherProjectId);
        otherProject.setProjectCode("PRJ-006");
        otherProject.setProjectName("Tower F");
        otherProject.setCustomerId(customerId);
        projectRepository.save(otherProject);

        ProjectBudgetLine line = new ProjectBudgetLine();
        line.setId(UUID.randomUUID());
        line.setProjectId(otherProjectId);
        line.setCostCode("EQP");
        line.setDescription("Equipment");
        line.setBudgetAmount(new BigDecimal("300.00"));
        budgetLineRepository.save(line);

        ProjectBudgetService service = new ProjectBudgetService(
                new ProjectService(projectRepository, customerRepository),
                budgetLineRepository);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.updateBudgetLine(
                projectId,
                line.getId(),
                new CreateProjectBudgetLineRequest("EQP", "Updated", new BigDecimal("350.00"))));

        assertEquals("Project budget line not found", exception.getMessage());
    }

    private static Customer activeCustomer(UUID customerId) {
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setCode("CUST-" + customerId.toString().substring(0, 8));
        customer.setName("Sample Customer");
        customer.setActive(true);
        return customer;
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
            return budgetLines.values().stream()
                    .filter(item -> projectId.equals(item.getProjectId()))
                    .toList();
        }

        @Override
        public void deleteById(UUID id) {
            budgetLines.remove(id);
        }
    }

    private static final class InMemoryCustomerRepository implements CustomerRepository {
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
