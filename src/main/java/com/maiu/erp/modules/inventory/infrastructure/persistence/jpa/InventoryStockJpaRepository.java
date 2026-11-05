package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.InventoryStockEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface InventoryStockJpaRepository
        extends JpaRepository<InventoryStockEntity, UUID> {

    Optional<InventoryStockEntity>
    findByProductIdAndWarehouseId(
            UUID productId,
            UUID warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from InventoryStockEntity s "
            + "where s.productId = :productId and s.warehouseId = :warehouseId")
    Optional<InventoryStockEntity>
    findByProductIdAndWarehouseIdForUpdate(
            @Param("productId") UUID productId,
            @Param("warehouseId") UUID warehouseId);

    List<InventoryStockEntity>
    findByProductId(UUID productId);

    List<InventoryStockEntity>
    findByWarehouseId(UUID warehouseId);
}
