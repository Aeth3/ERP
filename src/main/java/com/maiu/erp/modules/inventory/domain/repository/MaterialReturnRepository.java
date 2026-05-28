package com.maiu.erp.modules.inventory.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.model.MaterialReturn;

public interface MaterialReturnRepository {
    MaterialReturn save(MaterialReturn materialReturn);

    Optional<MaterialReturn> findById(UUID id);

    Optional<MaterialReturn> findByReturnNumber(String returnNumber);

    List<MaterialReturn> findAll();

    List<MaterialReturn> findByMaterialIssueId(UUID materialIssueId);
}
