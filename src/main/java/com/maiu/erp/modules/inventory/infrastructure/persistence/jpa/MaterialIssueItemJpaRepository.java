package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.MaterialIssueItemEntity;

public interface MaterialIssueItemJpaRepository extends JpaRepository<MaterialIssueItemEntity, UUID> {
    List<MaterialIssueItemEntity> findByMaterialIssueId(UUID materialIssueId);
}
