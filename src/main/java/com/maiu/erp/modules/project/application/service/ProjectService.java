package com.maiu.erp.modules.project.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;
import com.maiu.erp.modules.project.application.dto.CreateProjectRequest;
import com.maiu.erp.modules.project.application.dto.UpdateProjectRequest;
import com.maiu.erp.modules.project.domain.enums.ProjectStatus;
import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.modules.project.domain.repository.ProjectRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.ConflictException;
import com.maiu.erp.shared.exception.NotFoundException;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final CustomerRepository customerRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            CustomerRepository customerRepository) {
        this.projectRepository = projectRepository;
        this.customerRepository = customerRepository;
    }

    public UUID createProject(CreateProjectRequest request) {
        validateCustomerExists(request.customerId());
        String projectCode = normalizeRequired(request.projectCode(), "Project code is required");
        if (projectRepository.findByProjectCode(projectCode).isPresent()) {
            throw new ConflictException("Project code already exists");
        }

        Project project = new Project();
        project.setProjectCode(projectCode);
        project.setProjectName(normalizeRequired(request.projectName(), "Project name is required"));
        project.setCustomerId(request.customerId());
        project.setLocation(trimToNull(request.location()));
        project.setStartDate(request.startDate());
        project.setTargetEndDate(request.targetEndDate());
        project.setStatus(ProjectStatus.DRAFT);
        project.setCreatedAt(Instant.now());
        project.setUpdatedAt(Instant.now());

        return projectRepository.save(project).getId();
    }

    public List<Project> getProjects() {
        return projectRepository.findAll();
    }

    public Project getProjectById(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    public void updateProject(UUID projectId, UpdateProjectRequest request) {
        Project project = getProjectById(projectId);
        validateCustomerExists(request.customerId());
        project.setProjectName(normalizeRequired(request.projectName(), "Project name is required"));
        project.setCustomerId(request.customerId());
        project.setLocation(trimToNull(request.location()));
        project.setStartDate(request.startDate());
        project.setTargetEndDate(request.targetEndDate());
        project.setUpdatedAt(Instant.now());
        projectRepository.save(project);
    }

    public void activateProject(UUID projectId) {
        changeStatus(projectId, ProjectStatus.ACTIVE);
    }

    public void holdProject(UUID projectId) {
        changeStatus(projectId, ProjectStatus.ON_HOLD);
    }

    public void completeProject(UUID projectId) {
        changeStatus(projectId, ProjectStatus.COMPLETED);
    }

    public void cancelProject(UUID projectId) {
        changeStatus(projectId, ProjectStatus.CANCELLED);
    }

    private void changeStatus(UUID projectId, ProjectStatus status) {
        Project project = getProjectById(projectId);
        if (project.getStatus() == ProjectStatus.CANCELLED
                || project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BadRequestException("Project status can no longer be changed");
        }
        project.setStatus(status);
        project.setUpdatedAt(Instant.now());
        projectRepository.save(project);
    }

    private void validateCustomerExists(UUID customerId) {
        if (customerId == null) {
            throw new BadRequestException("Customer ID is required");
        }

        customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }

        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
