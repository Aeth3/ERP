package com.maiu.erp.modules.inventory.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.model.MaterialIssue;

public interface MaterialIssueRepository {
    MaterialIssue save(MaterialIssue materialIssue);

    Optional<MaterialIssue> findById(UUID id);

    Optional<MaterialIssue> findByIssueNumber(String issueNumber);

    List<MaterialIssue> findAll();

    List<MaterialIssue> findByProjectId(UUID projectId);
}
