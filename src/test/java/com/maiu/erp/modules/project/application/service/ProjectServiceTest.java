package com.maiu.erp.modules.project.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.inventory.domain.model.Customer;
import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;
import com.maiu.erp.modules.project.application.dto.CreateProjectRequest;
import com.maiu.erp.modules.project.domain.enums.ProjectStatus;
import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.modules.project.domain.repository.ProjectRepository;

class ProjectServiceTest {

    @Test
    void createProjectStartsAsDraft() {
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        UUID customerId = UUID.randomUUID();
        customerRepository.save(activeCustomer(customerId));

        ProjectService projectService = new ProjectService(projectRepository, customerRepository);

        UUID projectId = projectService.createProject(new CreateProjectRequest(
                "PRJ-001",
                "Warehouse Fitout",
                customerId,
                "Cebu",
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 7, 15)));

        Project project = projectRepository.findById(projectId).orElseThrow();
        assertEquals(ProjectStatus.DRAFT, project.getStatus());
        assertEquals("PRJ-001", project.getProjectCode());
        assertEquals("Warehouse Fitout", project.getProjectName());
    }

    @Test
    void createProjectFailsWhenCustomerDoesNotExist() {
        ProjectService projectService = new ProjectService(new InMemoryProjectRepository(), new InMemoryCustomerRepository());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> projectService.createProject(
                new CreateProjectRequest(
                        "PRJ-002",
                        "Site Office",
                        UUID.randomUUID(),
                        null,
                        null,
                        null)));

        assertEquals("Customer not found", exception.getMessage());
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
