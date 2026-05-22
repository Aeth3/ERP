package com.maiu.erp.modules.inventory.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.model.Unit;

public interface UnitRepository {
    Unit save(Unit unit);

    Optional<Unit> findById(UUID id);

    Optional<Unit> findByName(String name);

    List<Unit> findAll();

    List<Unit> findActiveUnits();
}
