package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.WarehouseEntity;

public interface WarehouseJpaRepository
        extends JpaRepository<WarehouseEntity, UUID> {

    Optional<WarehouseEntity> findByCode(String code);

    List<WarehouseEntity> findByActiveTrue();
}
