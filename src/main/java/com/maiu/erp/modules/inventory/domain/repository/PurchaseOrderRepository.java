package com.maiu.erp.modules.inventory.domain.repository;

import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import java.util.*;

public interface PurchaseOrderRepository {

    PurchaseOrder save(PurchaseOrder purchaseOrder);

    Optional<PurchaseOrder> findById(UUID id);

    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    List<PurchaseOrder> findAll();

    List<PurchaseOrder> findBySupplierId(UUID supplierId);

    List<PurchaseOrder> findByProjectId(UUID projectId);
}
