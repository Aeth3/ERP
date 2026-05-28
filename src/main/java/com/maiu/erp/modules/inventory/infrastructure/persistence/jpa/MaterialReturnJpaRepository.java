package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.MaterialReturnEntity;

public interface MaterialReturnJpaRepository extends JpaRepository<MaterialReturnEntity, UUID> {
    Optional<MaterialReturnEntity> findByReturnNumber(String returnNumber);

    List<MaterialReturnEntity> findByMaterialIssueId(UUID materialIssueId);
}
