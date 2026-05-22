package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.inventory.domain.model.Unit;
import com.maiu.erp.modules.inventory.domain.repository.UnitRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.UnitEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.UnitJpaRepository;

@Repository
public class UnitRepositoryImpl implements UnitRepository {

    private final UnitJpaRepository jpaRepository;

    public UnitRepositoryImpl(UnitJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Unit save(Unit unit) {
        return toDomain(jpaRepository.save(toEntity(unit)));
    }

    @Override
    public Optional<Unit> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Unit> findByName(String name) {
        return jpaRepository.findByNameIgnoreCase(name).map(this::toDomain);
    }

    @Override
    public List<Unit> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Unit> findActiveUnits() {
        return jpaRepository.findByActiveTrue().stream()
                .map(this::toDomain)
                .toList();
    }

    private Unit toDomain(UnitEntity entity) {
        Unit unit = new Unit();
        unit.setId(entity.getId());
        unit.setName(entity.getName());
        unit.setSymbol(entity.getSymbol());
        unit.setActive(entity.isActive());
        return unit;
    }

    private UnitEntity toEntity(Unit unit) {
        UnitEntity entity = new UnitEntity();
        entity.setId(unit.getId());
        entity.setName(unit.getName());
        entity.setSymbol(unit.getSymbol());
        entity.setActive(unit.isActive());
        return entity;
    }
}
