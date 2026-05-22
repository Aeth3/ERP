package com.maiu.erp.modules.inventory.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.model.Customer;
import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public UUID createCustomer(Customer customer) {
        if (customer.getCode() == null || customer.getCode().isBlank()) {
            throw new RuntimeException("Customer code is required");
        }
        if (customer.getName() == null || customer.getName().isBlank()) {
            throw new RuntimeException("Customer name is required");
        }
        if (customerRepository.findByCode(customer.getCode().trim()).isPresent()) {
            throw new RuntimeException("Customer code already exists");
        }
        if (customer.getActive() == null) {
            customer.setActive(true);
        }

        customer.setCode(customer.getCode().trim());
        customer.setName(customer.getName().trim());
        customer.setContactPerson(trimToNull(customer.getContactPerson()));
        customer.setPhone(trimToNull(customer.getPhone()));
        customer.setEmail(trimToNull(customer.getEmail()));
        customer.setAddress(trimToNull(customer.getAddress()));

        Customer saved = customerRepository.save(customer);
        return saved.getId();
    }

    public List<Customer> getCustomers(boolean activeOnly) {
        return activeOnly
                ? customerRepository.findActiveCustomers()
                : customerRepository.findAll();
    }

    public Customer getCustomerById(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
