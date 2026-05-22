package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.CustomerEntity;

public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, UUID> {
    Optional<CustomerEntity> findByCode(String code);

    List<CustomerEntity> findByActiveTrue();
}
