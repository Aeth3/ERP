package com.maiu.erp.modules.inventory.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.model.Customer;

public interface CustomerRepository {
    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);

    Optional<Customer> findByCode(String code);

    List<Customer> findAll();

    List<Customer> findActiveCustomers();
}
