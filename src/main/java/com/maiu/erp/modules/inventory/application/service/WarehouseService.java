package com.maiu.erp.modules.inventory.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.model.Warehouse;
import com.maiu.erp.modules.inventory.domain.repository.WarehouseRepository;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public UUID createWarehouse(Warehouse warehouse) {
        if (warehouse.getCode() == null || warehouse.getCode().isBlank()) {
            throw new RuntimeException("Warehouse code is required");
        }
        if (warehouse.getName() == null || warehouse.getName().isBlank()) {
            throw new RuntimeException("Warehouse name is required");
        }
        if (warehouseRepository.findByCode(warehouse.getCode()).isPresent()) {
            throw new RuntimeException("Warehouse code already exists");
        }
        if (warehouse.getActive() == null) {
            warehouse.setActive(true);
        }
        Warehouse saved = warehouseRepository.save(warehouse);
        return saved.getId();
    }

    public List<Warehouse> getWarehouses(boolean activeOnly) {
        return activeOnly
                ? warehouseRepository.findActiveWarehouses()
                : warehouseRepository.findAll();
    }

    public Warehouse getWarehouseById(UUID warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
    }
}
