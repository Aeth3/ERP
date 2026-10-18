package com.maiu.erp.modules.inventory.domain.repository;

import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import java.util.*;

public interface InventoryStockRepository  {

    InventoryStock save(InventoryStock stock);

    List<InventoryStock> findAll();

    Optional<InventoryStock> findByProductIdAndWarehouseId(
            UUID productId,
            UUID warehouseId);

    /**
     * Loads the stock row for a read-check-write update. Database-backed implementations must
     * hold a row lock until the surrounding transaction ends so concurrent stock changes
     * serialize instead of overwriting each other. The default suits single-threaded fakes.
     */
    default Optional<InventoryStock> findByProductIdAndWarehouseIdForUpdate(
            UUID productId,
            UUID warehouseId) {
        return findByProductIdAndWarehouseId(productId, warehouseId);
    }

    List<InventoryStock> findByProductId(UUID productId);

    List<InventoryStock> findByWarehouseId(UUID warehouseId);
}
