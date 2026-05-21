package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.SalesOrderEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesOrderJpaRepository
        extends JpaRepository<SalesOrderEntity, UUID> {

    Optional<SalesOrderEntity>
    findBySoNumber(String soNumber);

    List<SalesOrderEntity>
    findByCustomerId(UUID customerId);
}