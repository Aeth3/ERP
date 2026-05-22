package com.maiu.erp.modules.inventory.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.inventory.domain.model.Customer;
import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;

class CustomerServiceTest {

    @Test
    void createCustomerPersistsTrimmedCustomer() {
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        CustomerService customerService = new CustomerService(customerRepository);

        Customer customer = new Customer();
        customer.setCode("  CUS-001 ");
        customer.setName("  Acme Corp ");
        customer.setEmail("  billing@acme.test ");
        customer.setActive(true);

        UUID customerId = customerService.createCustomer(customer);

        Customer saved = customerRepository.findById(customerId).orElseThrow();
        assertNotNull(saved.getId());
        assertEquals("CUS-001", saved.getCode());
        assertEquals("Acme Corp", saved.getName());
        assertEquals("billing@acme.test", saved.getEmail());
    }

    @Test
    void createCustomerFailsWhenCodeAlreadyExists() {
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        customerRepository.save(customer("CUS-001", "Acme Corp"));
        CustomerService customerService = new CustomerService(customerRepository);

        Customer duplicate = new Customer();
        duplicate.setCode("CUS-001");
        duplicate.setName("Second Customer");
        duplicate.setActive(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> customerService.createCustomer(duplicate));

        assertEquals("Customer code already exists", exception.getMessage());
    }

    private static Customer customer(String code, String name) {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setCode(code);
        customer.setName(name);
        customer.setActive(true);
        return customer;
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
