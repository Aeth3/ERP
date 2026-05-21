package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.CategoryEntity;

public interface CategoryJpaRepository
        extends JpaRepository<CategoryEntity, UUID> {

    List<CategoryEntity> findByActiveTrue();

    List<CategoryEntity> findByParentId(UUID parentId);
}
