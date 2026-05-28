package com.maiu.erp.modules.inventory.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.inventory.application.dto.CategoryDto;
import com.maiu.erp.modules.inventory.application.dto.CreateCategoryRequest;
import com.maiu.erp.modules.inventory.application.dto.IdResponse;
import com.maiu.erp.modules.inventory.application.dto.UpdateCategoryRequest;
import com.maiu.erp.modules.inventory.application.service.CategoryService;
import com.maiu.erp.modules.inventory.domain.model.Category;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<IdResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        Category category = new Category();
        category.setName(request.name());
        category.setParentId(request.parentId());
        category.setActive(request.active() == null || request.active());

        UUID categoryId = categoryService.createCategory(category);

        return ResponseEntity.status(201).body(new IdResponse(categoryId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        Category category = new Category();
        category.setName(request.name());
        category.setParentId(request.parentId());
        category.setActive(request.active() == null || request.active());

        categoryService.updateCategory(id, category);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCategory(
            @PathVariable UUID id) {
        categoryService.deactivateCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateCategory(
            @PathVariable UUID id) {
        categoryService.activateCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(
                categoryService.getCategories(activeOnly).stream()
                        .map(this::toDto)
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                toDto(categoryService.getCategoryById(id)));
    }

    private CategoryDto toDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getParentId(),
                category.isActive());
    }
}
