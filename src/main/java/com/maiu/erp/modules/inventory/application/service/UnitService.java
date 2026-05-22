package com.maiu.erp.modules.inventory.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.model.Unit;
import com.maiu.erp.modules.inventory.domain.repository.UnitRepository;

@Service
public class UnitService {

    private final UnitRepository unitRepository;

    public UnitService(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    public UUID createUnit(Unit unit) {
        if (unit.getName() == null || unit.getName().isBlank()) {
            throw new RuntimeException("Unit name is required");
        }

        unitRepository.findByName(unit.getName().trim())
                .ifPresent(existing -> {
                    throw new RuntimeException("Unit name already exists");
                });

        unit.setName(unit.getName().trim());
        unit.setSymbol(unit.getSymbol() == null || unit.getSymbol().isBlank()
                ? null
                : unit.getSymbol().trim());
        unit.setActive(unit.isActive());

        Unit saved = unitRepository.save(unit);
        return saved.getId();
    }

    public List<Unit> getUnits(boolean activeOnly) {
        return activeOnly
                ? unitRepository.findActiveUnits()
                : unitRepository.findAll();
    }

    public Unit getUnitById(UUID unitId) {
        return unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found"));
    }
}
