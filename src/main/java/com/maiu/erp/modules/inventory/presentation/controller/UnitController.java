package com.maiu.erp.modules.inventory.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.inventory.application.dto.CreateUnitRequest;
import com.maiu.erp.modules.inventory.application.dto.IdResponse;
import com.maiu.erp.modules.inventory.application.dto.UnitDto;
import com.maiu.erp.modules.inventory.application.service.UnitService;
import com.maiu.erp.modules.inventory.domain.model.Unit;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/units")
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @PostMapping
    public ResponseEntity<IdResponse> createUnit(
            @Valid @RequestBody CreateUnitRequest request) {
        Unit unit = new Unit();
        unit.setName(request.name());
        unit.setSymbol(request.symbol());
        unit.setActive(request.active() == null || request.active());

        UUID unitId = unitService.createUnit(unit);
        return ResponseEntity.status(201).body(new IdResponse(unitId));
    }

    @GetMapping
    public ResponseEntity<List<UnitDto>> getUnits(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(
                unitService.getUnits(activeOnly).stream()
                        .map(this::toDto)
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnitDto> getUnitById(@PathVariable UUID id) {
        return ResponseEntity.ok(toDto(unitService.getUnitById(id)));
    }

    private UnitDto toDto(Unit unit) {
        return new UnitDto(
                unit.getId(),
                unit.getName(),
                unit.getSymbol(),
                unit.isActive());
    }
}
