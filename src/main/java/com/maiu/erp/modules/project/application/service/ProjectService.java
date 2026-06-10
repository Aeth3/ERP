package com.maiu.erp.modules.project.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;
import com.maiu.erp.modules.identity.application.security.PermissionCatalog;
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
    private static final Map<ProjectStatus, EnumSet<ProjectStatus>> ALLOWED_TRANSITIONS = Map.of(
            ProjectStatus.DRAFT, EnumSet.of(ProjectStatus.ACTIVE, ProjectStatus.CANCELLED),
            ProjectStatus.ACTIVE, EnumSet.of(ProjectStatus.ON_HOLD, ProjectStatus.COMPLETED, ProjectStatus.CANCELLED),
            ProjectStatus.ON_HOLD, EnumSet.of(ProjectStatus.ACTIVE, ProjectStatus.CANCELLED),
            ProjectStatus.COMPLETED, EnumSet.noneOf(ProjectStatus.class),
            ProjectStatus.CANCELLED, EnumSet.noneOf(ProjectStatus.class));

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
        project.setBudgetAmount(normalizeBudgetAmount(request.budgetAmount()));
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
        project.setBudgetAmount(normalizeBudgetAmount(request.budgetAmount()));
        project.setUpdatedAt(Instant.now());
        projectRepository.save(project);
    }

    @PreAuthorize("hasAuthority('" + PermissionCatalog.PROJECT_MANAGE + "')")
    public void activateProject(UUID projectId) {
        finalizeProjectActivation(projectId);
    }

    public void finalizeProjectActivation(UUID projectId) {
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
        ProjectStatus currentStatus = project.getStatus();
        if (currentStatus == status) {
            throw new BadRequestException("Project is already in status " + status.name());
        }

        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(ProjectStatus.class)).contains(status)) {
            throw new BadRequestException("Project status cannot move from " + currentStatus.name() + " to " + status.name());
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

    private BigDecimal normalizeBudgetAmount(BigDecimal budgetAmount) {
        if (budgetAmount == null) {
            return null;
        }

        if (budgetAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Budget amount must be zero or greater");
        }

        return budgetAmount;
    }
}
