package com.maiu.erp.modules.project.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.project.application.dto.ActionResponse;
import com.maiu.erp.modules.project.application.dto.CreateProjectRequest;
import com.maiu.erp.modules.project.application.dto.CreateProjectBudgetLineRequest;
import com.maiu.erp.modules.project.application.dto.IdResponse;
import com.maiu.erp.modules.project.application.dto.ProjectBudgetLineDto;
import com.maiu.erp.modules.project.application.dto.ProjectDto;
import com.maiu.erp.modules.project.application.dto.UpdateProjectRequest;
import com.maiu.erp.modules.project.application.service.ProjectBudgetService;
import com.maiu.erp.modules.project.application.service.ProjectService;
import com.maiu.erp.modules.project.domain.model.ProjectBudgetLine;
import com.maiu.erp.modules.project.domain.model.Project;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final ProjectBudgetService projectBudgetService;

    public ProjectController(ProjectService projectService, ProjectBudgetService projectBudgetService) {
        this.projectService = projectService;
        this.projectBudgetService = projectBudgetService;
    }

    @PostMapping
    public ResponseEntity<IdResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(201).body(new IdResponse(projectService.createProject(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActionResponse> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request) {
        projectService.updateProject(id, request);
        return ResponseEntity.ok(new ActionResponse("Project updated successfully"));
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getProjects() {
        return ResponseEntity.ok(projectService.getProjects().stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProject(@PathVariable UUID id) {
        return ResponseEntity.ok(toDto(projectService.getProjectById(id)));
    }

    @PostMapping("/{id}/budget-lines")
    public ResponseEntity<IdResponse> createBudgetLine(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProjectBudgetLineRequest request) {
        return ResponseEntity.status(201).body(new IdResponse(projectBudgetService.createBudgetLine(id, request)));
    }

    @GetMapping("/{id}/budget-lines")
    public ResponseEntity<List<ProjectBudgetLineDto>> getBudgetLines(@PathVariable UUID id) {
        return ResponseEntity.ok(projectBudgetService.getBudgetLines(id).stream().map(this::toBudgetLineDto).toList());
    }

    @PutMapping("/{id}/budget-lines/{budgetLineId}")
    public ResponseEntity<ActionResponse> updateBudgetLine(
            @PathVariable UUID id,
            @PathVariable UUID budgetLineId,
            @Valid @RequestBody CreateProjectBudgetLineRequest request) {
        projectBudgetService.updateBudgetLine(id, budgetLineId, request);
        return ResponseEntity.ok(new ActionResponse("Project budget line updated successfully"));
    }

    @DeleteMapping("/{id}/budget-lines/{budgetLineId}")
    public ResponseEntity<ActionResponse> deleteBudgetLine(
            @PathVariable UUID id,
            @PathVariable UUID budgetLineId) {
        projectBudgetService.deleteBudgetLine(id, budgetLineId);
        return ResponseEntity.ok(new ActionResponse("Project budget line deleted successfully"));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ActionResponse> activateProject(@PathVariable UUID id) {
        projectService.activateProject(id);
        return ResponseEntity.ok(new ActionResponse("Project activated successfully"));
    }

    @PostMapping("/{id}/hold")
    public ResponseEntity<ActionResponse> holdProject(@PathVariable UUID id) {
        projectService.holdProject(id);
        return ResponseEntity.ok(new ActionResponse("Project put on hold successfully"));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ActionResponse> completeProject(@PathVariable UUID id) {
        projectService.completeProject(id);
        return ResponseEntity.ok(new ActionResponse("Project completed successfully"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ActionResponse> cancelProject(@PathVariable UUID id) {
        projectService.cancelProject(id);
        return ResponseEntity.ok(new ActionResponse("Project cancelled successfully"));
    }

    private ProjectDto toDto(Project project) {
        return new ProjectDto(
                project.getId(),
                project.getProjectCode(),
                project.getProjectName(),
                project.getCustomerId(),
                project.getLocation(),
                project.getStartDate(),
                project.getTargetEndDate(),
                project.getBudgetAmount(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    private ProjectBudgetLineDto toBudgetLineDto(ProjectBudgetLine budgetLine) {
        return new ProjectBudgetLineDto(
                budgetLine.getId(),
                budgetLine.getProjectId(),
                budgetLine.getCostCode(),
                budgetLine.getDescription(),
                budgetLine.getBudgetAmount(),
                budgetLine.getCreatedAt(),
                budgetLine.getUpdatedAt());
    }
}
