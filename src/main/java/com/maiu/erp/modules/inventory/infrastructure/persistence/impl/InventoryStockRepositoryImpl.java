package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.repository.InventoryStockRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.InventoryStockEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.InventoryStockJpaRepository;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InventoryStockRepositoryImpl
        implements InventoryStockRepository {

    private final InventoryStockJpaRepository jpaRepository;

    public InventoryStockRepositoryImpl(
            InventoryStockJpaRepository jpaRepository) {

        this.jpaRepository = jpaRepository;
    }

    @Override
    public InventoryStock save(InventoryStock stock) {

        InventoryStockEntity entity = toEntity(stock);

        InventoryStockEntity saved =
                jpaRepository.save(entity);

        return toDomain(saved);
    }

    @Override
    public List<InventoryStock> findAll() {
        return jpaRepository
                .findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<InventoryStock> findByProductIdAndWarehouseId(
            UUID productId,
            UUID warehouseId) {

        return jpaRepository
                .findByProductIdAndWarehouseId(
                        productId,
                        warehouseId)
                .map(this::toDomain);
    }

    @Override
    public List<InventoryStock> findByProductId(
            UUID productId) {

        return jpaRepository
                .findByProductId(productId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<InventoryStock> findByWarehouseId(
            UUID warehouseId) {

        return jpaRepository
                .findByWarehouseId(warehouseId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private InventoryStock toDomain(
            InventoryStockEntity entity) {

        InventoryStock stock = new InventoryStock();

        stock.setId(entity.getId());
        stock.setProductId(entity.getProductId());
        stock.setWarehouseId(entity.getWarehouseId());

        stock.setQuantityOnHand(
                entity.getQuantityOnHand());

        stock.setReservedQuantity(
                entity.getReservedQuantity());

        stock.setReorderLevel(
                entity.getReorderLevel());

        stock.setUpdatedAt(
                entity.getUpdatedAt());

        return stock;
    }

    private InventoryStockEntity toEntity(
            InventoryStock stock) {

        InventoryStockEntity entity =
                new InventoryStockEntity();

        entity.setId(stock.getId());
        entity.setProductId(stock.getProductId());
        entity.setWarehouseId(stock.getWarehouseId());

        entity.setQuantityOnHand(
                stock.getQuantityOnHand());

        entity.setReservedQuantity(
                stock.getReservedQuantity() != null
                        ? stock.getReservedQuantity()
                        : BigDecimal.ZERO);

        entity.setReorderLevel(
                stock.getReorderLevel() != null
                        ? stock.getReorderLevel()
                        : BigDecimal.ZERO);

        entity.setUpdatedAt(
                stock.getUpdatedAt() != null
                        ? stock.getUpdatedAt()
                        : Instant.now());

        return entity;
    }
}
