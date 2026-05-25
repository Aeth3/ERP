package com.maiu.erp.modules.project.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.project.domain.model.Project;

public interface ProjectRepository {
    Project save(Project project);

    Optional<Project> findById(UUID id);

    Optional<Project> findByProjectCode(String projectCode);

    List<Project> findAll();
}
