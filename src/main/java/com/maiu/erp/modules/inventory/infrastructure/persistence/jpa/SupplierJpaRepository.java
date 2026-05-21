package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.SupplierEntity;

public interface SupplierJpaRepository
        extends JpaRepository<SupplierEntity, UUID> {

    Optional<SupplierEntity> findByCode(String code);

    List<SupplierEntity> findByActiveTrue();
}
