package com.maiu.erp.modules.inventory.domain.repository;

import com.maiu.erp.modules.inventory.domain.model.SalesOrder;
import java.util.*;

public interface SalesOrderRepository {

    SalesOrder save(SalesOrder salesOrder);

    Optional<SalesOrder> findById(UUID id);

    Optional<SalesOrder> findBySoNumber(String soNumber);

    List<SalesOrder> findAll();

    List<SalesOrder> findByCustomerId(UUID customerId);
}
