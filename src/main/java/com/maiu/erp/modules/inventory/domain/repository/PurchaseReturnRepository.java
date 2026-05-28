package com.maiu.erp.modules.inventory.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.model.PurchaseReturn;

public interface PurchaseReturnRepository {
    PurchaseReturn save(PurchaseReturn purchaseReturn);

    Optional<PurchaseReturn> findById(UUID id);

    Optional<PurchaseReturn> findByReturnNumber(String returnNumber);

    List<PurchaseReturn> findAll();

    List<PurchaseReturn> findByPurchaseOrderId(UUID purchaseOrderId);
}
