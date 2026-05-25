package com.maiu.erp.modules.inventory.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.model.Warehouse;
import com.maiu.erp.modules.inventory.domain.repository.InventoryStockRepository;
import com.maiu.erp.modules.inventory.domain.repository.WarehouseRepository;
import com.maiu.erp.shared.exception.BadRequestException;

class WarehouseServiceTest {

    @Test
    void deactivateWarehouseFailsWhenStockExists() {
        InMemoryWarehouseRepository warehouseRepository = new InMemoryWarehouseRepository();
        InMemoryInventoryStockRepository inventoryStockRepository = new InMemoryInventoryStockRepository();
        WarehouseService warehouseService = new WarehouseService(warehouseRepository, inventoryStockRepository);

        Warehouse warehouse = new Warehouse();
        warehouse.setCode("MAIN-WH");
        warehouse.setName("Main Warehouse");
        warehouse.setActive(true);
        UUID warehouseId = warehouseService.createWarehouse(warehouse);

        InventoryStock stock = new InventoryStock();
        stock.setProductId(UUID.randomUUID());
        stock.setWarehouseId(warehouseId);
        stock.setQuantityOnHand(new BigDecimal("3.00"));
        stock.setReservedQuantity(BigDecimal.ZERO);
        inventoryStockRepository.save(stock);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> warehouseService.deactivateWarehouse(warehouseId));

        assertEquals("Warehouse cannot be deactivated while stock exists", exception.getMessage());
    }

    private static final class InMemoryWarehouseRepository implements WarehouseRepository {
        private final Map<UUID, Warehouse> warehouses = new HashMap<>();

        @Override
        public Warehouse save(Warehouse warehouse) {
            if (warehouse.getId() == null) {
                warehouse.setId(UUID.randomUUID());
            }
            warehouses.put(warehouse.getId(), warehouse);
            return warehouse;
        }

        @Override
        public Optional<Warehouse> findById(UUID id) {
            return Optional.ofNullable(warehouses.get(id));
        }

        @Override
        public Optional<Warehouse> findByCode(String code) {
            return warehouses.values().stream().filter(warehouse -> code.equals(warehouse.getCode())).findFirst();
        }

        @Override
        public List<Warehouse> findAll() {
            return warehouses.values().stream().toList();
        }

        @Override
        public List<Warehouse> findActiveWarehouses() {
            return warehouses.values().stream().filter(warehouse -> Boolean.TRUE.equals(warehouse.getActive())).toList();
        }
    }

    private static final class InMemoryInventoryStockRepository implements InventoryStockRepository {
        private final Map<UUID, InventoryStock> stocks = new HashMap<>();

        @Override
        public InventoryStock save(InventoryStock stock) {
            if (stock.getId() == null) {
                stock.setId(UUID.randomUUID());
            }
            stocks.put(stock.getId(), stock);
            return stock;
        }

        @Override
        public Optional<InventoryStock> findByProductIdAndWarehouseId(UUID productId, UUID warehouseId) {
            return stocks.values().stream()
                    .filter(stock -> productId.equals(stock.getProductId()))
                    .filter(stock -> warehouseId.equals(stock.getWarehouseId()))
                    .findFirst();
        }

        @Override
        public List<InventoryStock> findByProductId(UUID productId) {
            return stocks.values().stream().filter(stock -> productId.equals(stock.getProductId())).toList();
        }

        @Override
        public List<InventoryStock> findByWarehouseId(UUID warehouseId) {
            return stocks.values().stream().filter(stock -> warehouseId.equals(stock.getWarehouseId())).toList();
        }
    }
}
