package com.maiu.erp.modules.project.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.project.infrastructure.persistence.entity.ProjectEntity;

public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, UUID> {
    Optional<ProjectEntity> findByProjectCode(String projectCode);
}
