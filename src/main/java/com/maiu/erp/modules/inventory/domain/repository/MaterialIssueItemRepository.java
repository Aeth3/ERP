package com.maiu.erp.modules.inventory.domain.repository;

import java.util.List;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.model.MaterialIssueItem;

public interface MaterialIssueItemRepository {
    MaterialIssueItem save(MaterialIssueItem item);

    List<MaterialIssueItem> findByMaterialIssueId(UUID materialIssueId);
}
