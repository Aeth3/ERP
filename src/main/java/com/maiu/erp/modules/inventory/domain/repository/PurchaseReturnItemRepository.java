package com.maiu.erp.modules.inventory.domain.repository;

import java.util.List;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.model.PurchaseReturnItem;

public interface PurchaseReturnItemRepository {
    PurchaseReturnItem save(PurchaseReturnItem item);

    List<PurchaseReturnItem> findByPurchaseReturnId(UUID purchaseReturnId);
}
