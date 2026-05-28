package com.maiu.erp.modules.inventory.domain.repository;

import java.util.List;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.model.SalesReturnItem;

public interface SalesReturnItemRepository {
    SalesReturnItem save(SalesReturnItem item);
    List<SalesReturnItem> findBySalesReturnId(UUID salesReturnId);
}
