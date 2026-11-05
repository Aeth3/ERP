package com.maiu.erp.modules.project.infrastructure.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.maiu.erp.modules.finance.application.port.ProjectBillingReader;
import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.modules.project.domain.repository.ProjectRepository;

@Component
public class FinanceProjectBillingReader implements ProjectBillingReader {
    private final ProjectRepository projectRepository;

    public FinanceProjectBillingReader(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public Optional<ProjectSnapshot> find(UUID projectId) {
        return projectRepository.findById(projectId).map(this::toSnapshot);
    }

    private ProjectSnapshot toSnapshot(Project project) {
        return new ProjectSnapshot(
                project.getId(),
                project.getProjectCode(),
                project.getProjectName(),
                project.getCustomerId(),
                project.getStatus().name(),
                project.getContractAmount(),
                project.getRetentionRate());
    }
}
