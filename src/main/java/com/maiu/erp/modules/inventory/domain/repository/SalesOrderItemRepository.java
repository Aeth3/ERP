package com.maiu.erp.modules.inventory.domain.repository;

import java.util.List;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.model.SalesOrderItem;

public interface SalesOrderItemRepository {

    SalesOrderItem save(SalesOrderItem item);

    List<SalesOrderItem> findBySalesOrderId(UUID salesOrderId);

    void deleteBySalesOrderId(UUID salesOrderId);
}