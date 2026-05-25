package com.maiu.erp.modules.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.MaterialIssueEntity;

public interface MaterialIssueJpaRepository extends JpaRepository<MaterialIssueEntity, UUID> {
    Optional<MaterialIssueEntity> findByIssueNumber(String issueNumber);

    List<MaterialIssueEntity> findByProjectId(UUID projectId);
}
