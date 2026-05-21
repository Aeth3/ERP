package com.maiu.erp.modules.identity.infrastructure.persistence.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
}