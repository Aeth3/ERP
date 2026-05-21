package com.maiu.erp.modules.inventory.domain.repository;

import com.maiu.erp.modules.inventory.domain.model.PurchaseOrderItem;
import java.util.*;

public interface PurchaseOrderItemRepository {

    PurchaseOrderItem save(PurchaseOrderItem item);

    List<PurchaseOrderItem> findByPurchaseOrderId(UUID purchaseOrderId);

    void deleteByPurchaseOrderId(UUID purchaseOrderId);
}
