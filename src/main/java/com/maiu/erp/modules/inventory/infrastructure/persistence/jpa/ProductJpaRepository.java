package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.ProductEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository
        extends JpaRepository<ProductEntity, UUID> {

    boolean existsBySku(String sku);

    Optional<ProductEntity> findBySku(String sku);

    List<ProductEntity> findByActiveTrue();
}