package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.StockMovementEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface StockMovementJpaRepository
        extends JpaRepository<StockMovementEntity, UUID> {

    List<StockMovementEntity> findByProductId(
            UUID productId);

    List<StockMovementEntity> findByWarehouseId(
            UUID warehouseId);

    List<StockMovementEntity> findByReferenceId(
            UUID referenceId);

    List<StockMovementEntity>
    findByMovementDateBetween(
            Instant start,
            Instant end);
}
