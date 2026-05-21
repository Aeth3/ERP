package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.InventoryStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface InventoryStockJpaRepository
        extends JpaRepository<InventoryStockEntity, UUID> {

    Optional<InventoryStockEntity>
    findByProductIdAndWarehouseId(
            UUID productId,
            UUID warehouseId);

    List<InventoryStockEntity>
    findByProductId(UUID productId);

    List<InventoryStockEntity>
    findByWarehouseId(UUID warehouseId);
}
