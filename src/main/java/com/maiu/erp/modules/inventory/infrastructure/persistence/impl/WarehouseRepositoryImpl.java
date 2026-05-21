package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.inventory.domain.model.Warehouse;
import com.maiu.erp.modules.inventory.domain.repository.WarehouseRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.WarehouseEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.WarehouseJpaRepository;

@Repository
public class WarehouseRepositoryImpl
        implements WarehouseRepository {

    private final WarehouseJpaRepository jpaRepository;

    public WarehouseRepositoryImpl(WarehouseJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        return toDomain(jpaRepository.save(toEntity(warehouse)));
    }

    @Override
    public Optional<Warehouse> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<Warehouse> findByCode(String code) {
        return jpaRepository.findByCode(code)
                .map(this::toDomain);
    }

    @Override
    public List<Warehouse> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Warehouse> findActiveWarehouses() {
        return jpaRepository.findByActiveTrue().stream()
                .map(this::toDomain)
                .toList();
    }

    private Warehouse toDomain(WarehouseEntity entity) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(entity.getId());
        warehouse.setCode(entity.getCode());
        warehouse.setName(entity.getName());
        warehouse.setAddress(entity.getAddress());
        warehouse.setActive(entity.getActive());
        return warehouse;
    }

    private WarehouseEntity toEntity(Warehouse warehouse) {
        WarehouseEntity entity = new WarehouseEntity();
        entity.setId(warehouse.getId());
        entity.setCode(warehouse.getCode());
        entity.setName(warehouse.getName());
        entity.setAddress(warehouse.getAddress());
        entity.setActive(warehouse.getActive());
        return entity;
    }
}
