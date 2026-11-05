package com.maiu.erp.modules.finance.testsupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.repository.InventoryStockRepository;

public final class InMemoryInventoryStockRepository implements InventoryStockRepository {
    private final List<InventoryStock> stocks = new ArrayList<>();

    @Override
    public InventoryStock save(InventoryStock stock) {
        stocks.add(stock);
        return stock;
    }

    @Override
    public List<InventoryStock> findAll() {
        return List.copyOf(stocks);
    }

    @Override
    public Optional<InventoryStock> findByProductIdAndWarehouseId(UUID productId, UUID warehouseId) {
        return Optional.empty();
    }

    @Override
    public List<InventoryStock> findByProductId(UUID productId) {
        return List.of();
    }

    @Override
    public List<InventoryStock> findByWarehouseId(UUID warehouseId) {
        return List.of();
    }
}
