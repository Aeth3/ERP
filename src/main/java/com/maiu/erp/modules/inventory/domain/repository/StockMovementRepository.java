package com.maiu.erp.modules.inventory.domain.repository;

import java.time.Instant;

import com.maiu.erp.modules.inventory.domain.model.StockMovement;

import java.util.*;

public interface StockMovementRepository {

    StockMovement save(StockMovement movement);

    List<StockMovement> findAll();

    List<StockMovement> findByProductId(UUID productId);

    List<StockMovement> findByWarehouseId(UUID warehouseId);

    List<StockMovement> findByProjectId(UUID projectId);

    List<StockMovement> findByReferenceId(UUID referenceId);

    List<StockMovement> findBetweenDates(
            Instant start,
            Instant end);
}
