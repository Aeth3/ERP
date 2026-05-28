package com.maiu.erp.modules.project.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.project.application.dto.ProjectMovementDto;
import com.maiu.erp.modules.project.application.dto.ProjectCostSummaryDto;
import com.maiu.erp.modules.project.application.dto.ProjectDashboardDto;
import com.maiu.erp.modules.project.application.service.ProjectCostQueryService;
import com.maiu.erp.modules.project.application.service.ProjectDashboardQueryService;
import com.maiu.erp.modules.project.application.service.ProjectMovementQueryService;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectCostController {
    private final ProjectCostQueryService projectCostQueryService;
    private final ProjectMovementQueryService projectMovementQueryService;
    private final ProjectDashboardQueryService projectDashboardQueryService;

    public ProjectCostController(
            ProjectCostQueryService projectCostQueryService,
            ProjectMovementQueryService projectMovementQueryService,
            ProjectDashboardQueryService projectDashboardQueryService) {
        this.projectCostQueryService = projectCostQueryService;
        this.projectMovementQueryService = projectMovementQueryService;
        this.projectDashboardQueryService = projectDashboardQueryService;
    }

    @GetMapping("/{id}/cost-summary")
    public ResponseEntity<ProjectCostSummaryDto> getProjectCostSummary(@PathVariable UUID id) {
        return ResponseEntity.ok(projectCostQueryService.getProjectCostSummary(id));
    }

    @GetMapping("/{id}/movements")
    public ResponseEntity<List<ProjectMovementDto>> getProjectMovements(@PathVariable UUID id) {
        return ResponseEntity.ok(projectMovementQueryService.getProjectMovements(id));
    }

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<ProjectDashboardDto> getProjectDashboard(@PathVariable UUID id) {
        return ResponseEntity.ok(projectDashboardQueryService.getProjectDashboard(id));
    }
}
