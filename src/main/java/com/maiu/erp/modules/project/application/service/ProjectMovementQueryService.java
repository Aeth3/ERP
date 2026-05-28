package com.maiu.erp.modules.project.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.model.MaterialReturn;
import com.maiu.erp.modules.inventory.domain.model.StockMovement;
import com.maiu.erp.modules.inventory.domain.repository.MaterialReturnRepository;
import com.maiu.erp.modules.inventory.domain.repository.StockMovementRepository;
import com.maiu.erp.modules.project.application.dto.ProjectMovementDto;

@Service
public class ProjectMovementQueryService {
    private final ProjectService projectService;
    private final StockMovementRepository stockMovementRepository;
    private final MaterialReturnRepository materialReturnRepository;

    public ProjectMovementQueryService(
            ProjectService projectService,
            StockMovementRepository stockMovementRepository,
            MaterialReturnRepository materialReturnRepository) {
        this.projectService = projectService;
        this.stockMovementRepository = stockMovementRepository;
        this.materialReturnRepository = materialReturnRepository;
    }

    public List<ProjectMovementDto> getProjectMovements(UUID projectId) {
        projectService.getProjectById(projectId);

        return stockMovementRepository.findByProjectId(projectId).stream()
                .sorted(Comparator.comparing(StockMovement::getMovementDate).reversed())
                .map(this::toDto)
                .toList();
    }

    private ProjectMovementDto toDto(StockMovement movement) {
        return new ProjectMovementDto(
                movement.getId(),
                movement.getProductId(),
                movement.getWarehouseId(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getUnitCost(),
                movement.getReferenceType(),
                movement.getReferenceId(),
                isReversalReturn(movement),
                movement.getRemarks(),
                movement.getPerformedBy(),
                movement.getMovementDate());
    }

    private boolean isReversalReturn(StockMovement movement) {
        if (!"MATERIAL_RETURN".equalsIgnoreCase(movement.getReferenceType()) || movement.getReferenceId() == null) {
            return false;
        }

        return materialReturnRepository.findById(movement.getReferenceId())
                .map(MaterialReturn::isReversal)
                .orElse(false);
    }
}
