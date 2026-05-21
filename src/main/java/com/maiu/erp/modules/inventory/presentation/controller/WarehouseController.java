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

import com.maiu.erp.modules.inventory.application.dto.CreateWarehouseRequest;
import com.maiu.erp.modules.inventory.application.dto.IdResponse;
import com.maiu.erp.modules.inventory.application.dto.WarehouseDto;
import com.maiu.erp.modules.inventory.application.service.WarehouseService;
import com.maiu.erp.modules.inventory.domain.model.Warehouse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PostMapping
    public ResponseEntity<IdResponse> createWarehouse(
            @Valid @RequestBody CreateWarehouseRequest request) {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode(request.code());
        warehouse.setName(request.name());
        warehouse.setAddress(request.address());
        warehouse.setActive(request.active() == null || request.active());

        UUID warehouseId = warehouseService.createWarehouse(warehouse);

        return ResponseEntity.status(201).body(new IdResponse(warehouseId));
    }

    @GetMapping
    public ResponseEntity<List<WarehouseDto>> getWarehouses(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(
                warehouseService.getWarehouses(activeOnly).stream()
                        .map(this::toDto)
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseDto> getWarehouseById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                toDto(warehouseService.getWarehouseById(id)));
    }

    private WarehouseDto toDto(Warehouse warehouse) {
        return new WarehouseDto(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.getActive());
    }
}
