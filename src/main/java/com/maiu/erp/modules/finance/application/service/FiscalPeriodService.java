package com.maiu.erp.modules.finance.application.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.finance.domain.enums.FiscalPeriodStatus;
import com.maiu.erp.modules.finance.domain.model.FiscalPeriod;
import com.maiu.erp.modules.finance.domain.repository.FiscalPeriodRepository;
import com.maiu.erp.shared.exception.BadRequestException;

import jakarta.transaction.Transactional;

/** Monthly calendar-year periods, created on first use. */
@Service
public class FiscalPeriodService {
    private final FiscalPeriodRepository fiscalPeriodRepository;

    public FiscalPeriodService(FiscalPeriodRepository fiscalPeriodRepository) {
        this.fiscalPeriodRepository = fiscalPeriodRepository;
    }

    /** The open period containing the date; refuses to post into a closed month. */
    @Transactional
    public FiscalPeriod openPeriodFor(LocalDate date) {
        FiscalPeriod period = findOrCreate(YearMonth.from(date));
        if (period.getStatus() == FiscalPeriodStatus.CLOSED) {
            throw new BadRequestException("Fiscal period " + label(period) + " is closed");
        }
        return period;
    }

    public List<FiscalPeriod> getPeriods() {
        return fiscalPeriodRepository.findAll().stream()
                .sorted(Comparator.comparingInt(FiscalPeriod::getYear)
                        .thenComparingInt(FiscalPeriod::getMonth)
                        .reversed())
                .toList();
    }

    @Transactional
    public FiscalPeriod closePeriod(int year, int month) {
        FiscalPeriod period = findOrCreate(toYearMonth(year, month));
        if (period.getStatus() == FiscalPeriodStatus.CLOSED) {
            throw new BadRequestException("Fiscal period " + label(period) + " is already closed");
        }
        period.setStatus(FiscalPeriodStatus.CLOSED);
        period.setClosedBy(CurrentActor.name());
        period.setClosedAt(Instant.now());
        return fiscalPeriodRepository.save(period);
    }

    @Transactional
    public FiscalPeriod reopenPeriod(int year, int month) {
        FiscalPeriod period = findOrCreate(toYearMonth(year, month));
        if (period.getStatus() == FiscalPeriodStatus.OPEN) {
            throw new BadRequestException("Fiscal period " + label(period) + " is already open");
        }
        period.setStatus(FiscalPeriodStatus.OPEN);
        period.setClosedBy(null);
        period.setClosedAt(null);
        return fiscalPeriodRepository.save(period);
    }

    private FiscalPeriod findOrCreate(YearMonth yearMonth) {
        return fiscalPeriodRepository.findByYearAndMonth(yearMonth.getYear(), yearMonth.getMonthValue())
                .orElseGet(() -> {
                    FiscalPeriod period = new FiscalPeriod();
                    period.setYear(yearMonth.getYear());
                    period.setMonth(yearMonth.getMonthValue());
                    period.setStartDate(yearMonth.atDay(1));
                    period.setEndDate(yearMonth.atEndOfMonth());
                    period.setStatus(FiscalPeriodStatus.OPEN);
                    return fiscalPeriodRepository.save(period);
                });
    }

    private YearMonth toYearMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("month must be between 1 and 12");
        }
        return YearMonth.of(year, month);
    }

    private String label(FiscalPeriod period) {
        return String.format("%d-%02d", period.getYear(), period.getMonth());
    }
}
