package com.maiu.erp.modules.inventory.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.inventory.application.dto.CreateMaterialIssueRequest;
import com.maiu.erp.modules.inventory.application.dto.IdResponse;
import com.maiu.erp.modules.inventory.application.dto.MaterialIssueDto;
import com.maiu.erp.modules.inventory.application.dto.MaterialIssueItemDto;
import com.maiu.erp.modules.inventory.application.service.MaterialIssueService;
import com.maiu.erp.modules.inventory.domain.model.MaterialIssue;
import com.maiu.erp.modules.inventory.domain.model.MaterialIssueItem;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/material-issues")
public class MaterialIssueController {
    private final MaterialIssueService materialIssueService;

    public MaterialIssueController(MaterialIssueService materialIssueService) {
        this.materialIssueService = materialIssueService;
    }

    @PostMapping
    public ResponseEntity<IdResponse> createMaterialIssue(@Valid @RequestBody CreateMaterialIssueRequest request) {
        return ResponseEntity.status(201).body(new IdResponse(materialIssueService.createMaterialIssue(request)));
    }

    @GetMapping
    public ResponseEntity<List<MaterialIssueDto>> getMaterialIssues() {
        return ResponseEntity.ok(materialIssueService.getMaterialIssues().stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialIssueDto> getMaterialIssueById(@PathVariable UUID id) {
        return ResponseEntity.ok(toDto(materialIssueService.getMaterialIssueById(id)));
    }

    private MaterialIssueDto toDto(MaterialIssue materialIssue) {
        return new MaterialIssueDto(
                materialIssue.getId(),
                materialIssue.getIssueNumber(),
                materialIssue.getProjectId(),
                materialIssue.getWarehouseId(),
                materialIssue.getRemarks(),
                materialIssue.getPerformedBy(),
                materialIssue.getIssuedAt(),
                materialIssueService.getMaterialIssueItems(materialIssue.getId()).stream().map(this::toItemDto).toList());
    }

    private MaterialIssueItemDto toItemDto(MaterialIssueItem item) {
        return new MaterialIssueItemDto(
                item.getId(),
                item.getMaterialIssueId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getLineTotal());
    }
}
