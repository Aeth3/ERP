package com.maiu.erp.modules.inventory.domain.repository;

import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import java.util.*;

public interface InventoryStockRepository  {

    InventoryStock save(InventoryStock stock);

    Optional<InventoryStock> findByProductIdAndWarehouseId(
            UUID productId,
            UUID warehouseId);

    List<InventoryStock> findByProductId(UUID productId);

    List<InventoryStock> findByWarehouseId(UUID warehouseId);
}
