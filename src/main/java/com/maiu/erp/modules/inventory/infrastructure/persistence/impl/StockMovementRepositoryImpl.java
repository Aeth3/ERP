package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import com.maiu.erp.modules.inventory.domain.model.StockMovement;
import com.maiu.erp.modules.inventory.domain.repository.StockMovementRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.StockMovementEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.StockMovementJpaRepository;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class StockMovementRepositoryImpl
        implements StockMovementRepository {

    private final StockMovementJpaRepository jpaRepository;

    public StockMovementRepositoryImpl(
            StockMovementJpaRepository jpaRepository) {

        this.jpaRepository = jpaRepository;
    }

    @Override
    public StockMovement save(StockMovement movement) {

        StockMovementEntity entity =
                toEntity(movement);

        StockMovementEntity saved =
                jpaRepository.save(entity);

        return toDomain(saved);
    }

    @Override
    public List<StockMovement> findAll() {
        return jpaRepository
                .findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<StockMovement> findByProductId(
            UUID productId) {

        return jpaRepository
                .findByProductId(productId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<StockMovement> findByWarehouseId(
            UUID warehouseId) {

        return jpaRepository
                .findByWarehouseId(warehouseId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<StockMovement> findByProjectId(
            UUID projectId) {

        return jpaRepository
                .findByProjectId(projectId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<StockMovement> findByReferenceId(
            UUID referenceId) {

        return jpaRepository
                .findByReferenceId(referenceId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<StockMovement> findBetweenDates(
            Instant start,
            Instant end) {

        return jpaRepository
                .findByMovementDateBetween(start, end)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private StockMovement toDomain(
            StockMovementEntity entity) {

        StockMovement movement =
                new StockMovement();

        movement.setId(entity.getId());

        movement.setProductId(
                entity.getProductId());

        movement.setWarehouseId(
                entity.getWarehouseId());

        movement.setProjectId(
                entity.getProjectId());

        movement.setMovementType(
                entity.getMovementType());

        movement.setQuantity(
                entity.getQuantity());

        movement.setUnitCost(
                entity.getUnitCost());

        movement.setReferenceType(
                entity.getReferenceType());

        movement.setReferenceId(
                entity.getReferenceId());

        movement.setRemarks(
                entity.getRemarks());

        movement.setPerformedBy(
                entity.getPerformedBy());

        movement.setMovementDate(
                entity.getMovementDate());

        return movement;
    }

    private StockMovementEntity toEntity(
            StockMovement movement) {

        StockMovementEntity entity =
                new StockMovementEntity();

        entity.setId(movement.getId());

        entity.setProductId(
                movement.getProductId());

        entity.setWarehouseId(
                movement.getWarehouseId());

        entity.setProjectId(
                movement.getProjectId());

        entity.setMovementType(
                movement.getMovementType());

        entity.setQuantity(
                movement.getQuantity());

        entity.setUnitCost(
                movement.getUnitCost());

        entity.setReferenceType(
                movement.getReferenceType());

        entity.setReferenceId(
                movement.getReferenceId());

        entity.setRemarks(
                movement.getRemarks());

        entity.setPerformedBy(
                movement.getPerformedBy());

        entity.setMovementDate(
                movement.getMovementDate());

        return entity;
    }
}
