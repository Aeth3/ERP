package com.maiu.erp.modules.finance.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.finance.infrastructure.persistence.entity.FiscalPeriodEntity;

public interface FiscalPeriodJpaRepository extends JpaRepository<FiscalPeriodEntity, UUID> {
    Optional<FiscalPeriodEntity> findByYearAndMonth(int year, int month);
}
