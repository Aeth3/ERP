package com.maiu.erp.modules.finance.testsupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.model.FiscalPeriod;
import com.maiu.erp.modules.finance.domain.repository.FiscalPeriodRepository;

public final class InMemoryFiscalPeriodRepository implements FiscalPeriodRepository {
    private final List<FiscalPeriod> periods = new ArrayList<>();

    @Override
    public FiscalPeriod save(FiscalPeriod period) {
        if (period.getId() == null) {
            period.setId(UUID.randomUUID());
            periods.add(period);
        }
        return period;
    }

    @Override
    public Optional<FiscalPeriod> findById(UUID id) {
        return periods.stream().filter(period -> id.equals(period.getId())).findFirst();
    }

    @Override
    public Optional<FiscalPeriod> findByYearAndMonth(int year, int month) {
        return periods.stream()
                .filter(period -> period.getYear() == year && period.getMonth() == month)
                .findFirst();
    }

    @Override
    public List<FiscalPeriod> findAll() {
        return List.copyOf(periods);
    }
}
