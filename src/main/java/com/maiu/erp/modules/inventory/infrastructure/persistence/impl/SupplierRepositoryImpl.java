package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.inventory.domain.model.Supplier;
import com.maiu.erp.modules.inventory.domain.repository.SupplierRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.SupplierEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.SupplierJpaRepository;

@Repository
public class SupplierRepositoryImpl
        implements SupplierRepository {

    private final SupplierJpaRepository jpaRepository;

    public SupplierRepositoryImpl(SupplierJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Supplier save(Supplier supplier) {
        return toDomain(jpaRepository.save(toEntity(supplier)));
    }

    @Override
    public Optional<Supplier> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<Supplier> findByCode(String code) {
        return jpaRepository.findByCode(code)
                .map(this::toDomain);
    }

    @Override
    public List<Supplier> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Supplier> findActiveSuppliers() {
        return jpaRepository.findByActiveTrue().stream()
                .map(this::toDomain)
                .toList();
    }

    private Supplier toDomain(SupplierEntity entity) {
        Supplier supplier = new Supplier();
        supplier.setId(entity.getId());
        supplier.setCode(entity.getCode());
        supplier.setName(entity.getName());
        supplier.setContactPerson(entity.getContactPerson());
        supplier.setPhone(entity.getPhone());
        supplier.setEmail(entity.getEmail());
        supplier.setAddress(entity.getAddress());
        supplier.setActive(entity.getActive());
        return supplier;
    }

    private SupplierEntity toEntity(Supplier supplier) {
        SupplierEntity entity = new SupplierEntity();
        entity.setId(supplier.getId());
        entity.setCode(supplier.getCode());
        entity.setName(supplier.getName());
        entity.setContactPerson(supplier.getContactPerson());
        entity.setPhone(supplier.getPhone());
        entity.setEmail(supplier.getEmail());
        entity.setAddress(supplier.getAddress());
        entity.setActive(supplier.getActive());
        return entity;
    }
}
