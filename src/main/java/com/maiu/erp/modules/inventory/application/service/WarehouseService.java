package com.maiu.erp.modules.inventory.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.model.Warehouse;
import com.maiu.erp.modules.inventory.domain.repository.InventoryStockRepository;
import com.maiu.erp.modules.inventory.domain.repository.WarehouseRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.ConflictException;
import com.maiu.erp.shared.exception.NotFoundException;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryStockRepository inventoryStockRepository;

    public WarehouseService(
            WarehouseRepository warehouseRepository,
            InventoryStockRepository inventoryStockRepository) {
        this.warehouseRepository = warehouseRepository;
        this.inventoryStockRepository = inventoryStockRepository;
    }

    public UUID createWarehouse(Warehouse warehouse) {
        validateWarehouse(warehouse, null);
        if (warehouse.getActive() == null) {
            warehouse.setActive(true);
        }
        Warehouse saved = warehouseRepository.save(warehouse);
        return saved.getId();
    }

    public void updateWarehouse(
            UUID warehouseId,
            Warehouse updatedWarehouse) {
        Warehouse existingWarehouse = getWarehouseById(warehouseId);
        validateWarehouse(updatedWarehouse, warehouseId);

        existingWarehouse.setCode(updatedWarehouse.getCode());
        existingWarehouse.setName(updatedWarehouse.getName());
        existingWarehouse.setAddress(updatedWarehouse.getAddress());
        existingWarehouse.setActive(updatedWarehouse.getActive() == null || updatedWarehouse.getActive());
        warehouseRepository.save(existingWarehouse);
    }

    public void deactivateWarehouse(
            UUID warehouseId) {
        Warehouse warehouse = getWarehouseById(warehouseId);

        boolean hasStock = inventoryStockRepository.findByWarehouseId(warehouseId)
                .stream()
                .anyMatch(this::hasInventoryBalance);

        if (hasStock) {
            throw new BadRequestException("Warehouse cannot be deactivated while stock exists");
        }

        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
    }

    public List<Warehouse> getWarehouses(boolean activeOnly) {
        return activeOnly
                ? warehouseRepository.findActiveWarehouses()
                : warehouseRepository.findAll();
    }

    public Warehouse getWarehouseById(UUID warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Warehouse not found"));
    }

    private void validateWarehouse(
            Warehouse warehouse,
            UUID warehouseId) {
        if (warehouse.getCode() == null || warehouse.getCode().isBlank()) {
            throw new BadRequestException("Warehouse code is required");
        }
        if (warehouse.getName() == null || warehouse.getName().isBlank()) {
            throw new BadRequestException("Warehouse name is required");
        }

        warehouseRepository.findByCode(warehouse.getCode())
                .filter(existing -> warehouseId == null || !warehouseId.equals(existing.getId()))
                .ifPresent(existing -> {
                    throw new ConflictException("Warehouse code already exists");
                });
    }

    private boolean hasInventoryBalance(
            InventoryStock stock) {
        return stock.getQuantityOnHand() != null && stock.getQuantityOnHand().signum() > 0
                || stock.getReservedQuantity() != null && stock.getReservedQuantity().signum() > 0;
    }
}
