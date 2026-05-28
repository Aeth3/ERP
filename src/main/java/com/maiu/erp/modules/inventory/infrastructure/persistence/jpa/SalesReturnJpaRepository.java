package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.SalesReturnEntity;

public interface SalesReturnJpaRepository extends JpaRepository<SalesReturnEntity, UUID> {
    Optional<SalesReturnEntity> findByReturnNumber(String returnNumber);
    List<SalesReturnEntity> findBySalesOrderId(UUID salesOrderId);
}
