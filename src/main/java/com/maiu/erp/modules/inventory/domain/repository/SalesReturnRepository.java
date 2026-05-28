package com.maiu.erp.modules.inventory.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.model.SalesReturn;

public interface SalesReturnRepository {
    SalesReturn save(SalesReturn salesReturn);
    Optional<SalesReturn> findById(UUID id);
    Optional<SalesReturn> findByReturnNumber(String returnNumber);
    List<SalesReturn> findAll();
    List<SalesReturn> findBySalesOrderId(UUID salesOrderId);
}
