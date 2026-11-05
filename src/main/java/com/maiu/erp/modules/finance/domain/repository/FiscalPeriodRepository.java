package com.maiu.erp.modules.finance.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.model.FiscalPeriod;

public interface FiscalPeriodRepository {
    FiscalPeriod save(FiscalPeriod period);

    Optional<FiscalPeriod> findById(UUID id);

    Optional<FiscalPeriod> findByYearAndMonth(int year, int month);

    List<FiscalPeriod> findAll();
}
