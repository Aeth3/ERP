package com.maiu.erp.modules.finance.infrastructure.persistence.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.finance.domain.model.FiscalPeriod;
import com.maiu.erp.modules.finance.domain.repository.FiscalPeriodRepository;
import com.maiu.erp.modules.finance.infrastructure.persistence.entity.FiscalPeriodEntity;
import com.maiu.erp.modules.finance.infrastructure.persistence.jpa.FiscalPeriodJpaRepository;

@Repository
public class FiscalPeriodRepositoryImpl implements FiscalPeriodRepository {
    private final FiscalPeriodJpaRepository jpaRepository;

    public FiscalPeriodRepositoryImpl(FiscalPeriodJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public FiscalPeriod save(FiscalPeriod period) {
        return toDomain(jpaRepository.save(toEntity(period)));
    }

    @Override
    public Optional<FiscalPeriod> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<FiscalPeriod> findByYearAndMonth(int year, int month) {
        return jpaRepository.findByYearAndMonth(year, month).map(this::toDomain);
    }

    @Override
    public List<FiscalPeriod> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private FiscalPeriod toDomain(FiscalPeriodEntity entity) {
        FiscalPeriod period = new FiscalPeriod();
        period.setId(entity.getId());
        period.setYear(entity.getYear());
        period.setMonth(entity.getMonth());
        period.setStartDate(entity.getStartDate());
        period.setEndDate(entity.getEndDate());
        period.setStatus(entity.getStatus());
        period.setClosedBy(entity.getClosedBy());
        period.setClosedAt(entity.getClosedAt());
        return period;
    }

    private FiscalPeriodEntity toEntity(FiscalPeriod period) {
        FiscalPeriodEntity entity = new FiscalPeriodEntity();
        entity.setId(period.getId());
        entity.setYear(period.getYear());
        entity.setMonth(period.getMonth());
        entity.setStartDate(period.getStartDate());
        entity.setEndDate(period.getEndDate());
        entity.setStatus(period.getStatus());
        entity.setClosedBy(period.getClosedBy());
        entity.setClosedAt(period.getClosedAt());
        return entity;
    }
}
