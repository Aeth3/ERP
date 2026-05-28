package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.SalesReturnItemEntity;

public interface SalesReturnItemJpaRepository extends JpaRepository<SalesReturnItemEntity, UUID> {
    List<SalesReturnItemEntity> findBySalesReturnId(UUID salesReturnId);
}
