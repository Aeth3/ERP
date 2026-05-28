package com.maiu.erp.modules.inventory.domain.repository;

import java.util.List;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.model.MaterialReturnItem;

public interface MaterialReturnItemRepository {
    MaterialReturnItem save(MaterialReturnItem item);

    List<MaterialReturnItem> findByMaterialReturnId(UUID materialReturnId);
}
