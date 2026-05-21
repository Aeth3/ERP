package com.maiu.erp.modules.inventory.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.model.Category;
import com.maiu.erp.modules.inventory.domain.repository.CategoryRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public UUID createCategory(Category category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new RuntimeException("Category name is required");
        }
        if (category.getParentId() != null) {
            categoryRepository.findById(category.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
        }
        category.setActive(category.isActive());
        Category saved = categoryRepository.save(category);
        return saved.getId();
    }

    public List<Category> getCategories(boolean activeOnly) {
        return activeOnly
                ? categoryRepository.findActiveCategories()
                : categoryRepository.findAll();
    }

    public Category getCategoryById(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }
}
