package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.UnitEntity;

public interface UnitJpaRepository extends JpaRepository<UnitEntity, UUID> {

    Optional<UnitEntity> findByNameIgnoreCase(String name);

    List<UnitEntity> findByActiveTrue();
}
