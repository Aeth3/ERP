package com.maiu.erp.modules.project.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.maiu.erp.modules.project.domain.enums.ProjectStatus;

public class ProjectDto {
    private final UUID id;
    private final String projectCode;
    private final String projectName;
    private final UUID customerId;
    private final String location;
    private final LocalDate startDate;
    private final LocalDate targetEndDate;
    private final ProjectStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ProjectDto(
            UUID id,
            String projectCode,
            String projectName,
            UUID customerId,
            String location,
            LocalDate startDate,
            LocalDate targetEndDate,
            ProjectStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.projectCode = projectCode;
        this.projectName = projectName;
        this.customerId = customerId;
        this.location = location;
        this.startDate = startDate;
        this.targetEndDate = targetEndDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public String getProjectName() {
        return projectName;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getLocation() {
        return location;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getTargetEndDate() {
        return targetEndDate;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
