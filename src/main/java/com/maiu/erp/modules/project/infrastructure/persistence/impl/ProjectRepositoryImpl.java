package com.maiu.erp.modules.project.infrastructure.persistence.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.modules.project.domain.repository.ProjectRepository;
import com.maiu.erp.modules.project.infrastructure.persistence.entity.ProjectEntity;
import com.maiu.erp.modules.project.infrastructure.persistence.jpa.ProjectJpaRepository;

@Repository
public class ProjectRepositoryImpl implements ProjectRepository {
    private final ProjectJpaRepository jpaRepository;

    public ProjectRepositoryImpl(ProjectJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Project save(Project project) {
        return toDomain(jpaRepository.save(toEntity(project)));
    }

    @Override
    public Optional<Project> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Project> findByProjectCode(String projectCode) {
        return jpaRepository.findByProjectCode(projectCode).map(this::toDomain);
    }

    @Override
    public List<Project> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private Project toDomain(ProjectEntity entity) {
        Project project = new Project();
        project.setId(entity.getId());
        project.setProjectCode(entity.getProjectCode());
        project.setProjectName(entity.getProjectName());
        project.setCustomerId(entity.getCustomerId());
        project.setLocation(entity.getLocation());
        project.setStartDate(entity.getStartDate());
        project.setTargetEndDate(entity.getTargetEndDate());
        project.setStatus(entity.getStatus());
        project.setCreatedAt(entity.getCreatedAt());
        project.setUpdatedAt(entity.getUpdatedAt());
        return project;
    }

    private ProjectEntity toEntity(Project project) {
        ProjectEntity entity = new ProjectEntity();
        entity.setId(project.getId());
        entity.setProjectCode(project.getProjectCode());
        entity.setProjectName(project.getProjectName());
        entity.setCustomerId(project.getCustomerId());
        entity.setLocation(project.getLocation());
        entity.setStartDate(project.getStartDate());
        entity.setTargetEndDate(project.getTargetEndDate());
        entity.setStatus(project.getStatus());
        entity.setCreatedAt(project.getCreatedAt());
        entity.setUpdatedAt(project.getUpdatedAt());
        return entity;
    }
}
