package com.maiu.erp.modules.project.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.project.application.dto.ActionResponse;
import com.maiu.erp.modules.project.application.dto.CreateProjectRequest;
import com.maiu.erp.modules.project.application.dto.IdResponse;
import com.maiu.erp.modules.project.application.dto.ProjectDto;
import com.maiu.erp.modules.project.application.dto.UpdateProjectRequest;
import com.maiu.erp.modules.project.application.service.ProjectService;
import com.maiu.erp.modules.project.domain.model.Project;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
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
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
