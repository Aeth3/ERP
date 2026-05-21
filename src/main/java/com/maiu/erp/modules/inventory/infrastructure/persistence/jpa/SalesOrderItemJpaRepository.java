package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.SalesOrderItemEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SalesOrderItemJpaRepository
        extends JpaRepository<SalesOrderItemEntity, UUID> {

    List<SalesOrderItemEntity>
    findBySalesOrderId(UUID salesOrderId);

    void deleteBySalesOrderId(UUID salesOrderId);
}