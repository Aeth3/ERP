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

import com.maiu.erp.modules.inventory.domain.model.Unit;
import com.maiu.erp.modules.inventory.domain.repository.UnitRepository;

class UnitServiceTest {

    @Test
    void createUnitPersistsTrimmedUnit() {
        InMemoryUnitRepository unitRepository = new InMemoryUnitRepository();
        UnitService unitService = new UnitService(unitRepository);

        Unit unit = new Unit();
        unit.setName("  Piece  ");
        unit.setSymbol("  pc ");
        unit.setActive(true);

        UUID unitId = unitService.createUnit(unit);

        Unit saved = unitRepository.findById(unitId).orElseThrow();
        assertNotNull(saved.getId());
        assertEquals("Piece", saved.getName());
        assertEquals("pc", saved.getSymbol());
        assertEquals(true, saved.isActive());
    }

    @Test
    void createUnitFailsWhenNameAlreadyExists() {
        InMemoryUnitRepository unitRepository = new InMemoryUnitRepository();
        unitRepository.save(unit("Piece", "pc"));
        UnitService unitService = new UnitService(unitRepository);

        Unit duplicate = new Unit();
        duplicate.setName("piece");
        duplicate.setActive(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> unitService.createUnit(duplicate));

        assertEquals("Unit name already exists", exception.getMessage());
    }

    private static Unit unit(String name, String symbol) {
        Unit unit = new Unit();
        unit.setId(UUID.randomUUID());
        unit.setName(name);
        unit.setSymbol(symbol);
        unit.setActive(true);
        return unit;
    }

    private static final class InMemoryUnitRepository implements UnitRepository {
        private final Map<UUID, Unit> units = new HashMap<>();

        @Override
        public Unit save(Unit unit) {
            if (unit.getId() == null) {
                unit.setId(UUID.randomUUID());
            }
            units.put(unit.getId(), unit);
            return unit;
        }

        @Override
        public Optional<Unit> findById(UUID id) {
            return Optional.ofNullable(units.get(id));
        }

        @Override
        public Optional<Unit> findByName(String name) {
            return units.values().stream()
                    .filter(unit -> name.equalsIgnoreCase(unit.getName()))
                    .findFirst();
        }

        @Override
        public List<Unit> findAll() {
            return units.values().stream().toList();
        }

        @Override
        public List<Unit> findActiveUnits() {
            return units.values().stream()
                    .filter(Unit::isActive)
                    .toList();
        }
    }
}
