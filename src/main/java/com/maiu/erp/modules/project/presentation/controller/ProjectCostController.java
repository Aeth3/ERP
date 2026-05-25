package com.maiu.erp.modules.project.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.project.application.dto.ProjectCostSummaryDto;
import com.maiu.erp.modules.project.application.service.ProjectCostQueryService;

@RestController
@RequestMapping("/projects")
public class ProjectCostController {
    private final ProjectCostQueryService projectCostQueryService;

    public ProjectCostController(ProjectCostQueryService projectCostQueryService) {
        this.projectCostQueryService = projectCostQueryService;
    }

    @GetMapping("/{id}/cost-summary")
    public ResponseEntity<ProjectCostSummaryDto> getProjectCostSummary(@PathVariable UUID id) {
        return ResponseEntity.ok(projectCostQueryService.getProjectCostSummary(id));
    }
}
