package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.MaterialReturnItemEntity;

public interface MaterialReturnItemJpaRepository extends JpaRepository<MaterialReturnItemEntity, UUID> {
    List<MaterialReturnItemEntity> findByMaterialReturnId(UUID materialReturnId);
}
