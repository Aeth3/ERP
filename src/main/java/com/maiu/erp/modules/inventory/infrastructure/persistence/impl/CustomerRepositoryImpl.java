package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.inventory.domain.model.Customer;
import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.CustomerEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.CustomerJpaRepository;

@Repository
public class CustomerRepositoryImpl implements CustomerRepository {

    private final CustomerJpaRepository jpaRepository;

    public CustomerRepositoryImpl(CustomerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Customer save(Customer customer) {
        return toDomain(jpaRepository.save(toEntity(customer)));
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Customer> findByCode(String code) {
        return jpaRepository.findByCode(code).map(this::toDomain);
    }

    @Override
    public List<Customer> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Customer> findActiveCustomers() {
        return jpaRepository.findByActiveTrue().stream()
                .map(this::toDomain)
                .toList();
    }

    private Customer toDomain(CustomerEntity entity) {
        Customer customer = new Customer();
        customer.setId(entity.getId());
        customer.setCode(entity.getCode());
        customer.setName(entity.getName());
        customer.setContactPerson(entity.getContactPerson());
        customer.setPhone(entity.getPhone());
        customer.setEmail(entity.getEmail());
        customer.setAddress(entity.getAddress());
        customer.setActive(entity.getActive());
        return customer;
    }

    private CustomerEntity toEntity(Customer customer) {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(customer.getId());
        entity.setCode(customer.getCode());
        entity.setName(customer.getName());
        entity.setContactPerson(customer.getContactPerson());
        entity.setPhone(customer.getPhone());
        entity.setEmail(customer.getEmail());
        entity.setAddress(customer.getAddress());
        entity.setActive(customer.getActive());
        return entity;
    }
}
